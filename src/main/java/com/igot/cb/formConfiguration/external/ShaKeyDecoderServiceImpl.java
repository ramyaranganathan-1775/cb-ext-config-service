package com.igot.cb.formConfiguration.external;

import com.igot.cb.util.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

import static com.igot.cb.util.Constants.PROFILE_TOKEN_HMAC_ALGORITHM;
import static com.igot.cb.util.Constants.PROFILE_TOKEN_KEY_DIGEST;

/**
 * Sample payload this decodes (fields not listed here are ignored):
 * <pre>{@code
 * {
 *   "designation": "designation1",
 *   "user": "a707c493-5d12-4ac5-ab61-228433be6edd",
 *   "rootorgid": "0140788510336040962",
 *   "ministryOrStateType": "ministry",
 *   "roles": ["VOLUNTEER"],
 *   "iss": "sunbird-lms-service",
 *   "iat": 1788776806
 * }
 * }</pre>
 * No {@code exp} claim as of this writing, so verification here is signature-only — jjwt doesn't
 * enforce an expiry check when the claim itself is absent from the token.
 * <p>
 * Every failure mode (missing/blank key, bad signature, malformed token, missing required claims)
 * is swallowed here and surfaced as {@link Optional#empty()}, never an exception — a caller-supplied
 * sha key problem must fall back to the existing org-read/user-read call path, not fail the request.
 */
@Service
@Slf4j
public class ShaKeyDecoderServiceImpl implements ShaKeyDecoderService {

    private static final String CLAIM_ROOT_ORG_ID = "rootorgid";
    private static final String CLAIM_USER = "user";

    /** Shared HMAC secret this must match exactly with sunbird-lms-service's signing key. */
    @Value("${formconfig.shaKey.secret:}")
    private String secret;

    /**
     * Tolerance for clock drift between sunbird-lms-service (which sets iat/exp) and this service.
     * An expired sha key (exp + skew in the past) is rejected like any other invalid one.
     */
    @Value("${formconfig.shaKey.clockSkewSeconds:60}")
    private long clockSkewSeconds = 60;

    /**
     * lms sets exp (iat + sunbird_profile_token_expiration) since KB-15286 #269. When true, a sha key
     * without exp (issued by an older lms build) is rejected instead of being trusted forever.
     */
    @Value("${formconfig.shaKey.requireExpiry:true}")
    private boolean requireExpiry = true;

    @Override
    public Optional<ShaKeyProfile> decode(String shaKey) {
        if (StringUtils.isBlank(shaKey)) {
            return Optional.empty();
        }
        if (StringUtils.isBlank(secret)) {
            log.warn("ShaKeyDecoderServiceImpl: formconfig.shaKey.secret is not configured — ignoring sha key");
            return Optional.empty();
        }
        try {
            byte[] keyBytes = MessageDigest.getInstance(PROFILE_TOKEN_KEY_DIGEST)
                    .digest(secret.trim().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .setSigningKey(new SecretKeySpec(keyBytes, PROFILE_TOKEN_HMAC_ALGORITHM))
                    .requireIssuer(Constants.SHA_KEY_ISSUER)
                    .setAllowedClockSkewSeconds(clockSkewSeconds)
                    .parseClaimsJws(shaKey)
                    .getBody();

            if (requireExpiry && claims.getExpiration() == null) {
                log.warn("ShaKeyDecoderServiceImpl: sha key has no exp claim — ignoring");
                return Optional.empty();
            }

            String userId = claims.get(CLAIM_USER, String.class);
            String rootOrgId = claims.get(CLAIM_ROOT_ORG_ID, String.class);
            String ministryOrStateType = claims.get(Constants.MINISTRY_OR_STATE_TYPE, String.class);
            String designation = claims.get(Constants.DESIGNATION, String.class);

            if (StringUtils.isBlank(userId) || StringUtils.isBlank(rootOrgId)) {
                log.warn("ShaKeyDecoderServiceImpl: decoded sha key is missing '{}'/'{}' — ignoring",
                        CLAIM_USER, CLAIM_ROOT_ORG_ID);
                return Optional.empty();
            }

            return Optional.of(ShaKeyProfile.builder()
                    .userId(userId)
                    .rootOrgId(rootOrgId)
                    .ministryOrStateType(ministryOrStateType)
                    .designations(StringUtils.isNotBlank(designation) ? List.of(designation) : List.of())
                    .build());
        } catch (ExpiredJwtException e) {
            // Signature already verified by this point - the token is genuine, just past its exp.
            log.info("ShaKeyDecoderServiceImpl: sha key expired at {}", e.getClaims().getExpiration());
            throw new ShaKeyExpiredException(e.getClaims().get(CLAIM_USER, String.class));
        } catch (JwtException | IllegalArgumentException | java.security.NoSuchAlgorithmException e) {
            log.warn("ShaKeyDecoderServiceImpl: failed to decode/verify sha key: {}", e.getMessage());
            return Optional.empty();
        }
    }
}

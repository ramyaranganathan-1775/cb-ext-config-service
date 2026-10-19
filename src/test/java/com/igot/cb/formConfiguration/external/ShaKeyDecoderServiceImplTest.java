package com.igot.cb.formConfiguration.external;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.igot.cb.util.Constants.PROFILE_TOKEN_KEY_DIGEST;
import static org.junit.jupiter.api.Assertions.*;

class ShaKeyDecoderServiceImplTest {

    private static final String SECRET = "test-secret-value-for-sha-key-hmac";

    private ShaKeyDecoderServiceImpl newService(String secret) {
        ShaKeyDecoderServiceImpl service = new ShaKeyDecoderServiceImpl();
        ReflectionTestUtils.setField(service, "secret", secret);
        return service;
    }

    private String signedToken(String secret, Map<String, Object> claims) {
        byte[] keyBytes = new byte[0];
        try {
            keyBytes = MessageDigest.getInstance(PROFILE_TOKEN_KEY_DIGEST)
                    .digest(secret.trim().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            throw new RuntimeException("Unable to derive PR-compatible sha key", ignored);
        }
        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS256, new SecretKeySpec(keyBytes, "HmacSHA256"))
                .compact();
    }

    private Map<String, Object> samplePayload() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("designation", "designation1");
        claims.put("user", "a707c493-5d12-4ac5-ab61-228433be6edd");
        claims.put("rootorgid", "0140788510336040962");
        claims.put("ministryOrStateType", "ministry");
        claims.put("roles", java.util.List.of("VOLUNTEER"));
        claims.put("iss", "sunbird-lms-service");
        long now = System.currentTimeMillis() / 1000;
        claims.put("iat", now);
        claims.put("exp", now + 3600);
        return claims;
    }

    @Test
    void decodesValidTokenIntoProfile() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        String token = signedToken(SECRET, samplePayload());

        Optional<ShaKeyProfile> result = service.decode(token);

        assertTrue(result.isPresent());
        ShaKeyProfile profile = result.get();
        assertEquals("a707c493-5d12-4ac5-ab61-228433be6edd", profile.getUserId());
        assertEquals("0140788510336040962", profile.getRootOrgId());
        assertEquals("ministry", profile.getMinistryOrStateType());
        assertEquals(java.util.List.of("designation1"), profile.getDesignations());
    }

    @Test
    void wrongSigningSecretIsRejected() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        String token = signedToken("a-completely-different-secret", samplePayload());

        assertTrue(service.decode(token).isEmpty());
    }

    @Test
    void blankShaKeyIsEmpty() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        assertTrue(service.decode(null).isEmpty());
        assertTrue(service.decode("").isEmpty());
        assertTrue(service.decode("   ").isEmpty());
    }

    @Test
    void malformedTokenIsEmpty() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        assertTrue(service.decode("not-a-real-jwt").isEmpty());
    }

    @Test
    void unconfiguredSecretIsEmpty() {
        ShaKeyDecoderServiceImpl service = newService("");
        String token = signedToken(SECRET, samplePayload());

        assertTrue(service.decode(token).isEmpty());
    }

    @Test
    void missingRequiredClaimIsEmpty() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        Map<String, Object> claims = samplePayload();
        claims.remove("rootorgid");
        String token = signedToken(SECRET, claims);

        assertTrue(service.decode(token).isEmpty());
    }

    @Test
    void missingDesignationYieldsEmptyList() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        Map<String, Object> claims = samplePayload();
        claims.remove("designation");
        String token = signedToken(SECRET, claims);

        Optional<ShaKeyProfile> result = service.decode(token);
        assertTrue(result.isPresent());
        assertTrue(result.get().getDesignations().isEmpty());
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        Map<String, Object> claims = samplePayload();
        claims.put("iss", "someone-else");

        assertTrue(service.decode(signedToken(SECRET, claims)).isEmpty());
    }

    @Test
    void rejectsTokenWithoutIssuer() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        Map<String, Object> claims = samplePayload();
        claims.remove("iss");

        assertTrue(service.decode(signedToken(SECRET, claims)).isEmpty());
    }

    @Test
    void rejectsExpiredToken() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        Map<String, Object> claims = samplePayload();
        long now = System.currentTimeMillis() / 1000;
        claims.put("iat", now - 7200);
        claims.put("exp", now - 3600);

        ShaKeyExpiredException e = assertThrows(ShaKeyExpiredException.class,
                () -> service.decode(signedToken(SECRET, claims)));
        assertEquals(claims.get("user"), e.getUserId());
    }

    /** Same claims as a real lms profileToken (KB-15286): blank designation, SPV org type, 1-hour validity. */
    private Map<String, Object> lmsProfileTokenClaims(long iat) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("profilestatus", "NOT-VERIFIED");
        claims.put("service", "");
        claims.put("batch", "");
        claims.put("cadre", "");
        claims.put("designation", "");
        claims.put("user", "8155bb04-3b07-475a-8584-8881bc9f81bf");
        claims.put("rootorgid", "01384675536621568015");
        claims.put("group", "");
        claims.put("ministryOrStateId", "0135071359030722569");
        claims.put("ministryOrStateType", "SPV");
        claims.put("roles", java.util.List.of("AI_ASSESSMENT_CREATOR", "CONTENT_CREATOR", "PUBLIC"));
        claims.put("iss", "sunbird-lms-service");
        claims.put("iat", iat);
        claims.put("exp", iat + 3600);
        return claims;
    }

    @Test
    void lmsProfileToken_withinItsHour_decodes() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        long now = System.currentTimeMillis() / 1000;

        ShaKeyProfile profile = service.decode(signedToken(SECRET, lmsProfileTokenClaims(now - 1800))).orElseThrow();

        assertEquals("8155bb04-3b07-475a-8584-8881bc9f81bf", profile.getUserId());
        assertEquals("01384675536621568015", profile.getRootOrgId());
        assertEquals("SPV", profile.getMinistryOrStateType());
        assertTrue(profile.getDesignations().isEmpty());
    }

    @Test
    void lmsProfileToken_afterItsHour_throwsExpired() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        long now = System.currentTimeMillis() / 1000;
        String token = signedToken(SECRET, lmsProfileTokenClaims(now - 3600 - 120));

        ShaKeyExpiredException e = assertThrows(ShaKeyExpiredException.class, () -> service.decode(token));
        assertEquals("8155bb04-3b07-475a-8584-8881bc9f81bf", e.getUserId());
    }

    @Test
    void expiredTokenWithBadSignature_isRejectedNotReportedAsExpired() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        long now = System.currentTimeMillis() / 1000;
        String forged = signedToken("a-different-secret-of-at-least-32-chars!!", lmsProfileTokenClaims(now - 7200));

        assertTrue(service.decode(forged).isEmpty());
    }

    @Test
    void acceptsTokenExpiredWithinClockSkew() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        Map<String, Object> claims = samplePayload();
        claims.put("exp", System.currentTimeMillis() / 1000 - 10);

        assertTrue(service.decode(signedToken(SECRET, claims)).isPresent());
    }

    @Test
    void rejectsTokenWithoutExpiry_whenExpiryRequired() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        Map<String, Object> claims = samplePayload();
        claims.remove("exp");

        assertTrue(service.decode(signedToken(SECRET, claims)).isEmpty());
    }

    @Test
    void acceptsTokenWithoutExpiry_whenExpiryNotRequired() {
        ShaKeyDecoderServiceImpl service = newService(SECRET);
        ReflectionTestUtils.setField(service, "requireExpiry", false);
        Map<String, Object> claims = samplePayload();
        claims.remove("exp");

        assertTrue(service.decode(signedToken(SECRET, claims)).isPresent());
    }
}

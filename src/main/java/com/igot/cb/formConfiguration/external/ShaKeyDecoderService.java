package com.igot.cb.formConfiguration.external;

import java.util.Optional;

/**
 * Decodes and verifies the optional "sha key" a caller may pass on {@code /formsConfig/read} — an
 * HMAC-signed JWT (issued by sunbird-lms-service) carrying that user's rootOrgId,
 * ministryOrStateType and designation, already resolved. When it decodes to a trusted profile,
 * {@code FormsConfigurationServiceImpl} uses it in place of both the {@code OrgReadService} and
 * {@code UserDesignationService} external calls for that request.
 * <p>
 * {@link #decode} returns {@link Optional#empty()} for a missing key, bad signature, malformed token
 * or wrong/missing claims, so the caller can fall back to the external-call path exactly as if no
 * sha key had been sent at all. The one exception is an expired key (valid signature, exp in the
 * past): that throws {@link ShaKeyExpiredException}, so the UI can be told to fetch a new token.
 */
public interface ShaKeyDecoderService {
    /**
     * @throws ShaKeyExpiredException if the key is genuine but past its exp (beyond the clock skew).
     */
    Optional<ShaKeyProfile> decode(String shaKey);
}

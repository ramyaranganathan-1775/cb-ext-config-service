package com.igot.cb.formConfiguration.external;

import com.igot.cb.util.Constants;

/**
 * Thrown by {@link ShaKeyDecoderService#decode} when the sha key (lms profileToken) has a valid
 * signature but its {@code exp} has passed. Unlike every other sha key problem, this one is
 * reported to the caller, so the UI can refresh and fetch a new profileToken.
 */
public class ShaKeyExpiredException extends RuntimeException {

    /** The expired token's "user" claim — so the caller can check it was issued for them. */
    private final String userId;

    public ShaKeyExpiredException(String userId) {
        super(Constants.ResponseMessages.PROFILE_TOKEN_EXPIRED);
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}

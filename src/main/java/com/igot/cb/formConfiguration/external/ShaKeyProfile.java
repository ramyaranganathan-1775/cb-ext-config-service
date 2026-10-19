package com.igot.cb.formConfiguration.external;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * The subset of a decoded/verified sha-key JWT's claims that {@code FormsConfigurationServiceImpl}
 * needs to resolve a {@code /formsConfig/read} request — see {@link ShaKeyDecoderService}. Whenever
 * this is available (and trusted — see {@link ShaKeyDecoderServiceImpl}), it replaces what would
 * otherwise require an {@code OrgReadService} call (rootOrgId + ministryOrStateType) and a
 * {@code UserDesignationService} call (designations) combined.
 */
@Getter
@Builder
public class ShaKeyProfile {
    /** The "user" claim — cross-checked against the caller's own userId before this is trusted. */
    private final String userId;
    /** The "rootorgid" claim. */
    private final String rootOrgId;
    /** The "ministryOrStateType" claim, as issued (may be e.g. "SPV" - see {@link MinistryOrStateTypeResolver}). */
    private final String ministryOrStateType;
    /** The "sbOrgType" claim - the fallback when ministryOrStateType isn't "ministry"/"state". */
    private final String sbOrgType;
    /** The "designation" claim, wrapped into a single-element list (never null; empty if blank/absent). */
    private final List<String> designations;
}

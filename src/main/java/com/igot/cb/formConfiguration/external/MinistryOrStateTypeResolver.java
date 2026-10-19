package com.igot.cb.formConfiguration.external;

import com.igot.cb.util.Constants;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 * Works out a user's organisation type ("ministry" / "state") for the designation rule, from the
 * org's {@code ministryOrStateType} and {@code sbOrgType}. One rule shared by both sources of these
 * values - the org read API ({@link OrgReadServiceImpl}, used by /formsConfig/read) and the lms
 * profileToken (used by /formsConfig/read/v2) - so both endpoints resolve the same org the same way.
 */
public final class MinistryOrStateTypeResolver {

    private MinistryOrStateTypeResolver() {
    }

    /**
     * Ministry first, then state:
     * <ol>
     *   <li>{@code ministryOrStateType} if it is "ministry" or "state";</li>
     *   <li>otherwise (blank, or a value like "SPV") {@code sbOrgType} if it is "ministry" or "state";</li>
     *   <li>otherwise null - the designation rule doesn't apply.</li>
     * </ol>
     *
     * @return "ministry", "state" (always lower case) or null
     */
    public static String resolve(String ministryOrStateType, String sbOrgType) {
        if (isMinistryOrState(ministryOrStateType)) {
            return ministryOrStateType.toLowerCase(Locale.ROOT);
        }
        if (isMinistryOrState(sbOrgType)) {
            return sbOrgType.toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private static boolean isMinistryOrState(String value) {
        return StringUtils.isNotBlank(value)
                && (Constants.MINISTRY.equalsIgnoreCase(value) || Constants.STATE.equalsIgnoreCase(value));
    }
}

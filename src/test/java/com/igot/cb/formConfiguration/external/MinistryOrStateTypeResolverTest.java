package com.igot.cb.formConfiguration.external;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MinistryOrStateTypeResolverTest {

    @Test
    void ministryOrStateTypeIsUsedWhenItIsMinistryOrState() {
        assertEquals("ministry", MinistryOrStateTypeResolver.resolve("ministry", "state"));
        assertEquals("state", MinistryOrStateTypeResolver.resolve("State", null));
        assertEquals("ministry", MinistryOrStateTypeResolver.resolve("MINISTRY", null));
    }

    @Test
    void spvFallsBackToSbOrgType() {
        assertEquals("state", MinistryOrStateTypeResolver.resolve("SPV", "state"));
        assertEquals("ministry", MinistryOrStateTypeResolver.resolve("SPV", "Ministry"));
    }

    @Test
    void blankMinistryOrStateTypeFallsBackToSbOrgType() {
        assertEquals("state", MinistryOrStateTypeResolver.resolve("", "state"));
        assertEquals("ministry", MinistryOrStateTypeResolver.resolve(null, "ministry"));
    }

    @Test
    void neitherMinistryNorState_isNull() {
        assertNull(MinistryOrStateTypeResolver.resolve("SPV", null));
        assertNull(MinistryOrStateTypeResolver.resolve("SPV", "mdo"));
        assertNull(MinistryOrStateTypeResolver.resolve("mdo", ""));
        assertNull(MinistryOrStateTypeResolver.resolve(null, null));
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RuleDomainTest {

    @Test
    void validationCreateNewInitializesActiveWithTimestamps() {
        Validation validation = Validation.createNew("CODE", "DESC", ValidationDataType.TEXT, "creator");

        assertEquals("A", validation.status());
        assertNotNull(validation.createdAt());
        assertEquals(validation.createdAt(), validation.updatedAt());
        assertEquals("creator", validation.updatedBy());
    }

    @Test
    void validationChangeStatusUpdatesStatusAndTimestamp() {
        Validation original = Validation.rehydrate(1L, "CODE", "DESC", ValidationDataType.BOOL, "A",
                OffsetDateTime.now().minusDays(1), null, OffsetDateTime.now().minusDays(1), OffsetDateTime.now().minusDays(1), "u1");

        Validation updated = original.changeStatus("I", "u2");

        assertEquals("I", updated.status());
        assertEquals("u2", updated.updatedBy());
        assertTrue(updated.updatedAt().isAfter(original.updatedAt()) || updated.updatedAt().isEqual(original.updatedAt()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "X", "ACTIVE"})
    void validationRejectsInvalidStatus(String newStatus) {
        Validation original = Validation.createNew("CODE", "DESC", ValidationDataType.TEXT, "creator");
        assertThrows(IllegalArgumentException.class, () -> original.changeStatus(newStatus, "u"));
    }

    @Test
    void validationUpdateBasicsChangesDescriptionAndAudit() {
        Validation original = Validation.createNew("CODE", "DESC", ValidationDataType.TEXT, "creator");

        Validation updated = original.updateBasics("NEW DESC", "upd");

        assertEquals("NEW DESC", updated.description());
        assertEquals("upd", updated.updatedBy());
        assertTrue(updated.updatedAt().isAfter(original.updatedAt()) || updated.updatedAt().isEqual(original.updatedAt()));
    }

    @Test
    void validationMapCreateNewSetsActiveStatus() {
        ValidationMap map = ValidationMap.createNew("ST", "123456", 10L, "SI", null, null, "creator");

        assertEquals("A", map.status());
        assertEquals("SI", map.valueFlag());
        assertEquals("creator", map.updatedBy());
    }

    @Test
    void validationMapRequiresMandatoryFields() {
        assertThrows(IllegalArgumentException.class, () -> ValidationMap.createNew(null, "123456", 1L, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> ValidationMap.createNew("ST", null, 1L, null, null, null, null));
        assertThrows(NullPointerException.class, () -> ValidationMap.createNew("ST", "123456", null, null, null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "X", "ACTIVE"})
    void validationMapRejectsInvalidStatus(String status) {
        ValidationMap map = ValidationMap.createNew("ST", "123456", 1L, null, null, null, "actor");
        assertThrows(IllegalArgumentException.class, () -> map.changeStatus(status, "u"));
    }

    @Test
    void validationMapChangeStatusUpdatesAudit() {
        ValidationMap map = ValidationMap.createNew("ST", "123456", 1L, null, null, null, "actor");

        ValidationMap updated = map.changeStatus("I", "upd");

        assertEquals("I", updated.status());
        assertEquals("upd", updated.updatedBy());
        assertNotNull(updated.updatedAt());
        assertTrue(updated.updatedAt().isAfter(map.createdAt()) || updated.updatedAt().isEqual(map.createdAt()));
    }
}

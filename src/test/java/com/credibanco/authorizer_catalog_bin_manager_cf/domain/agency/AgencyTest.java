package com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AgencyTest {

    @Test
    void createNewInitializesMandatoryFields() {
        Agency agency = Agency.createNew(
                "SUB", "01", "Main",
                "123", "Street", "555", "05001",
                "HL", "1234",
                "JOHN", "123", "JANE", "234",
                "MARY", "345", "MIKE", "456",
                "Desc", "actor"
        );

        assertEquals("SUB", agency.subtypeCode());
        assertEquals("01", agency.agencyCode());
        assertEquals("Main", agency.name());
        assertEquals("A", agency.status());
        assertNotNull(agency.createdAt());
        assertNotNull(agency.updatedAt());
        assertEquals("actor", agency.updatedBy());
    }

    @Test
    void updateBasicsKeepsStatusAndCreatedAt() {
        OffsetDateTime created = OffsetDateTime.now().minusDays(1);
        OffsetDateTime updated = created.plusHours(1);
        Agency existing = Agency.rehydrate(
                "SUB", "01", "Main",
                "123", "Street", "555", "05001",
                "HL", "1234",
                "JOHN", "123", "JANE", "234",
                "MARY", "345", "MIKE", "456",
                "Desc", "A", created, updated, "actor"
        );

        Agency refreshed = existing.updateBasics(
                "New name", "987", "New st", "999", "05001",
                "HL2", "5678",
                "AA", "111", "BB", "222",
                "CC", "333", "DD", "444",
                "New desc", "editor"
        );

        assertEquals("A", refreshed.status());
        assertEquals(created, refreshed.createdAt());
        assertEquals("editor", refreshed.updatedBy());
        assertTrue(refreshed.updatedAt().isAfter(updated));
    }

    @Test
    void changeStatusValidatesAllowedValues() {
        Agency agency = Agency.createNew(
                "SUB", "01", "Main",
                "123", null, null, null,
                null, null,
                null, null, null, null,
                null, null, null, null,
                null, null
        );

        Agency inactive = agency.changeStatus("I", "actor");
        assertEquals("I", inactive.status());
        assertEquals("actor", inactive.updatedBy());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agency.changeStatus("X", "actor"));
        assertTrue(ex.getMessage().contains("status"));
    }

    @Test
    void nameIsRequiredOnConstruction() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new Agency("SUB", "01", "", null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null,
                        null, "A", OffsetDateTime.now(), OffsetDateTime.now(), null));
        assertTrue(ex.getMessage().contains("name"));
    }
}

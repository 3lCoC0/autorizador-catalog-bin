package com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtypeTest {

    @Test
    void createNewShouldNormalizeValues() {
        Subtype subtype = Subtype.createNew("ABC", "123-456", "Name", "Desc", "CC", "123", "7", "user");
        assertEquals("123456", subtype.bin());
        assertEquals("7", subtype.binExt());
        assertEquals("1234567", subtype.binEfectivo());
        assertEquals("I", subtype.status());
    }

    @Test
    void updateBasicsShouldRefreshFields() {
        Subtype original = Subtype.createNew("ABC", "123456", "Name", "Desc", null, null, "7", "user");
        Subtype updated = original.updateBasics("New", "Desc2", "TI", "456", "9", "admin");
        assertEquals("New", updated.name());
        assertEquals("Desc2", updated.description());
        assertEquals("9", updated.binExt());
        assertEquals("1234569", updated.binEfectivo());
        assertNotNull(updated.updatedAt());
    }

    @Test
    void changeStatusShouldValidateInput() {
        Subtype original = Subtype.createNew("ABC", "123456", "Name", "Desc", null, null, null, "user");
        Subtype active = original.changeStatus("A", "admin");
        assertEquals("A", active.status());
        assertThrows(IllegalArgumentException.class, () -> original.changeStatus("X", "admin"));
    }

    @Test
    void rehydrateShouldValidateConsistency() {
        OffsetDateTime now = OffsetDateTime.now();
        assertThrows(IllegalArgumentException.class, () ->
                Subtype.rehydrate("ABC", "123456", "Name", "Desc", "A",
                        null, null, "1", "999999", 1L, now, now, null));
    }

    @Test
    void shouldValidateBinLengthAndRequiredFields() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                Subtype.createNew("AB", "123456", "", "Desc", null, null, null, "user"));
        assertTrue(ex.getMessage().contains("name"));

        assertThrows(IllegalArgumentException.class, () ->
                Subtype.createNew("ABC", "12345", "Name", "Desc", null, null, null, "user"));
        assertThrows(IllegalArgumentException.class, () ->
                Subtype.createNew("ABC", "1234567890", "Name", "Desc", null, null, null, "user"));
    }

    @Test
    void digitsOnlyOrNullShouldRemoveNonDigits() {
        Subtype subtype = Subtype.createNew("ABC", "123456", "Name", "Desc", null, null, "  a", "user");
        assertEquals("123456", subtype.bin());
        assertNull(subtype.binExt());
        assertEquals("", Subtype.computeBinEfectivo("", null));
    }
}

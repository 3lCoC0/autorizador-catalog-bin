package com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SubtypeDomainTest {

    @Test
    void createNewNormalizesDigitsAndInitializesInactiveStatus() {
        Subtype subtype = Subtype.createNew("ABC", "12-34 56", "Nombre", "Desc", "CC", "123", " 9 ", "creator");

        assertEquals("ABC", subtype.subtypeCode());
        assertEquals("123456", subtype.bin());
        assertEquals("Nombre", subtype.name());
        assertEquals("Desc", subtype.description());
        assertEquals("I", subtype.status());
        assertEquals("CC", subtype.ownerIdType());
        assertEquals("123", subtype.ownerIdNumber());
        assertEquals("9", subtype.binExt());
        assertEquals("1234569", subtype.binEfectivo());
        assertNull(subtype.subtypeId());
        assertNull(subtype.createdAt());
        assertNull(subtype.updatedAt());
        assertEquals("creator", subtype.updatedBy());
    }

    @Test
    void updateBasicsAppliesNewValuesAndRecomputesEffectiveBin() {
        Subtype original = Subtype.createNew("ABC", "123456", "Nombre", "Desc", "CC", "123", null, null);
        Subtype updated = original.updateBasics("Nuevo", "Nueva desc", "TI", "999", "x1y2z3", "modifier");

        assertEquals("Nuevo", updated.name());
        assertEquals("Nueva desc", updated.description());
        assertEquals("TI", updated.ownerIdType());
        assertEquals("999", updated.ownerIdNumber());
        assertEquals("123456", updated.bin());
        assertEquals("123456123", updated.binEfectivo());
        assertEquals("123", updated.binExt());
        assertEquals("modifier", updated.updatedBy());
        assertEquals(original.subtypeCode(), updated.subtypeCode());
        assertEquals(original.status(), updated.status());
        assertNull(updated.subtypeId());
        assertNotNull(updated.updatedAt());
    }

    @Test
    void changeStatusUpdatesStatusAndTimestamp() {
        Subtype subtype = Subtype.createNew("ABC", "123456", "Nombre", "Desc", "CC", "123", null, null);
        Subtype updated = subtype.changeStatus("A", "approver");

        assertEquals("A", updated.status());
        assertEquals("approver", updated.updatedBy());
        assertNotNull(updated.updatedAt());
        assertEquals(subtype.binEfectivo(), updated.binEfectivo());
    }

    @Test
    void updateBasicsClearsBinExtensionWhenBlank() {
        Subtype subtype = Subtype.createNew("ABC", "123456", "Nombre", "Desc", "CC", "123", "99", null);
        Subtype updated = subtype.updateBasics("Nombre", "Desc", "CC", "123", "   ", "modifier");

        assertNull(updated.binExt());
        assertEquals("123456", updated.binEfectivo());
    }

    @Test
    void updateBasicsRequiresNonBlankName() {
        Subtype subtype = Subtype.createNew("ABC", "123456", "Nombre", "Desc", "CC", "123", null, null);

        assertThrows(IllegalArgumentException.class, () ->
                subtype.updateBasics("  ", "Desc", "CC", "123", null, "modifier"));
    }

    @Test
    void rehydrateCreatesValidInstance() {
        OffsetDateTime now = OffsetDateTime.now();
        Subtype subtype = Subtype.rehydrate(
                "ABC", "123456", "Nombre", "Desc", "A",
                "CC", "123", "12", "12345612", 10L, now, now, "auditor");

        assertEquals(10L, subtype.subtypeId());
        assertEquals(now, subtype.createdAt());
        assertEquals(now, subtype.updatedAt());
        assertEquals("auditor", subtype.updatedBy());
    }

    @ParameterizedTest
    @CsvSource({
            " ,123456,Nombre,Desc,A",        // subtype code required
            "AB,123456,Nombre,Desc,A",       // subtype code length
            "ABCD,123456,Nombre,Desc,A",     // subtype code length
            "ABC,12345,Nombre,Desc,A",       // bin too short
            "ABC,1234567890,Nombre,Desc,A",  // bin too long
            "ABC,1234a6,Nombre,Desc,A",      // bin not numeric
            "ABC,123456, ,Desc,A",           // name required
            "ABC,123456,Nombre,Desc,X"       // invalid status
    })
    void constructorValidationsRejectInvalidData(String code, String bin, String name, String desc, String status) {
        assertThrows(IllegalArgumentException.class, () ->
                new Subtype(code, bin, name, desc, status,
                        null, null, null, bin, 1L,
                        OffsetDateTime.now(), OffsetDateTime.now(), null));
    }

    @Test
    void constructorRejectsInconsistentEffectiveBin() {
        assertThrows(IllegalArgumentException.class, () ->
                new Subtype("ABC", "123456", "Nombre", "Desc", "A",
                        null, null, "1", "123456", 1L,
                        OffsetDateTime.now(), OffsetDateTime.now(), null));
    }

    @Test
    void changeStatusKeepsIdentityAndExtValues() {
        OffsetDateTime created = OffsetDateTime.now().minusDays(1);
        Subtype subtype = Subtype.rehydrate("ABC", "123456", "Nombre", "Desc", "I",
                "CC", "123", "07", "12345607", 99L, created, created, "creator");

        Subtype updated = subtype.changeStatus("A", "approver");

        assertEquals(subtype.subtypeCode(), updated.subtypeCode());
        assertEquals(subtype.bin(), updated.bin());
        assertEquals("07", updated.binExt());
        assertEquals("12345607", updated.binEfectivo());
        assertEquals(subtype.subtypeId(), updated.subtypeId());
        assertEquals(created, updated.createdAt());
        assertEquals("A", updated.status());
        assertEquals("approver", updated.updatedBy());
        assertNotNull(updated.updatedAt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", "", "AA"})
    void changeStatusRejectsInvalidStatuses(String status) {
        Subtype subtype = Subtype.createNew("ABC", "123456", "Nombre", "Desc", "CC", "123", null, null);
        assertThrows(IllegalArgumentException.class, () -> subtype.changeStatus(status, "user"));
    }
}

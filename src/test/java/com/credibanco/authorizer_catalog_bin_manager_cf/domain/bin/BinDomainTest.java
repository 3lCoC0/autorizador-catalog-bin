package com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BinDomainTest {

    @Test
    void createNewInitializesWithActiveStatusAndTimestamps() {
        Bin bin = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, "creator");
        assertEquals("A", bin.status());
        assertNotNull(bin.createdAt());
        assertEquals(bin.createdAt(), bin.updatedAt());
        assertEquals("creator", bin.updatedBy());
    }

    @ParameterizedTest
    @CsvSource({
            "123456,NAME,DEBITO,12,CC,DESC,Y,1",
            "1234567,NAME,PREPAGO,12,CC,DESC,N,''"
    })
    void changeStatusValidatesNewStatus(String binValue, String name, String typeBin, String typeAcc,
                                         String comp, String desc, String usesExt, String digitsStr) {
        Integer digits = (digitsStr == null || digitsStr.isBlank()) ? null : Integer.valueOf(digitsStr);
        Bin bin = Bin.rehydrate(binValue, name, typeBin, typeAcc, comp, desc, "A", OffsetDateTime.now(), OffsetDateTime.now(), null, usesExt, digits);
        Bin updated = bin.changeStatus("I", "user");
        assertEquals("I", updated.status());
        assertEquals("user", updated.updatedBy());
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", "", "AA"})
    void changeStatusRejectsInvalidStatuses(String status) {
        Bin bin = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null);
        assertThrows(IllegalArgumentException.class, () -> bin.changeStatus(status, "u"));
    }

    @Test
    void updateBasicsUpdatesFieldsAndTimestamps() {
        Bin bin = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null);
        Bin updated = bin.updateBasics("NEW", "CREDITO", "34", "NC", "NEW DESC", "Y", 2, "user");
        assertEquals("NEW", updated.name());
        assertEquals("CREDITO", updated.typeBin());
        assertEquals("34", updated.typeAccount());
        assertEquals("Y", updated.usesBinExt());
        assertEquals(2, updated.binExtDigits());
        assertEquals("user", updated.updatedBy());
        assertTrue(updated.updatedAt().isAfter(bin.updatedAt()) || updated.updatedAt().isEqual(bin.updatedAt()));
    }

    @ParameterizedTest
    @CsvSource({
            " ,DEBITO,12",
            "NEW,OTRO,12",
            "NEW,DEBITO,1A"
    })
    void updateBasicsValidatesInputs(String name, String typeBin, String typeAccount) {
        Bin bin = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null);

        assertThrows(IllegalArgumentException.class,
                () -> bin.updateBasics(name, typeBin, typeAccount, "NC", "DESC", "N", null, "user"));
    }

    @Test
    void updateBasicsEnforcesExtendedBinLimits() {
        Bin bin = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "Y", 1, null);

        assertThrows(IllegalArgumentException.class,
                () -> bin.updateBasics("NEW", "DEBITO", "12", "NC", "DESC", "Y", 4, "user"));
    }

    @ParameterizedTest
    @CsvSource({
            "12345,NAME,DEBITO,12,CC,DESC,N," ,
            "123456, ,DEBITO,12,CC,DESC,N,",
            "123456,NAME,OTRO,12,CC,DESC,N,",
            "123456,NAME,DEBITO,1,CC,DESC,N,",
            "123456,NAME,DEBITO,12,CC,DESC,Y,4",
            "123456,NAME,DEBITO,12,CC,DESC,N,1"
    })
    void constructorGuardsAgainstInvalidData(String binVal, String name, String typeBin, String typeAcc,
                                             String comp, String desc, String usesExt, String digitsStr) {
        Integer digits = (digitsStr == null || digitsStr.isBlank()) ? null : Integer.valueOf(digitsStr);
        assertThrows(IllegalArgumentException.class, () ->
                Bin.createNew(binVal, name, typeBin, typeAcc, comp, desc, usesExt, digits, null));
    }
}

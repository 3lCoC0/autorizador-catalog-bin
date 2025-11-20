package com.credibanco.authorizer_catalog_bin_manager_cf.shared.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextNormalizerTest {

    @Test
    void convertsToUppercaseAndStripsAccents() {
        String normalized = TextNormalizer.uppercaseAndRemoveAccents(" áRbol ñandú ");
        assertEquals("ARBOL NANDU", normalized);
    }

    @Test
    void returnsEmptyStringForBlankInput() {
        assertEquals("", TextNormalizer.uppercaseAndRemoveAccents("   \t"));
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(TextNormalizer.uppercaseAndRemoveAccents((String) null));
    }

    @Test
    void normalizesArraysInPlace() {
        String[] values = {"á", null, " caFé"};
        TextNormalizer.uppercaseAndRemoveAccents(values);
        assertArrayEquals(new String[]{"A", null, "CAFE"}, values);
    }
}

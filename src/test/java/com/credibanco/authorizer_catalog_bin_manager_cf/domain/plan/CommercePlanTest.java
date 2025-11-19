package com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommercePlanTest {

    @Test
    void createNewEnforcesRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> CommercePlan.createNew(null, "NAME", CommerceValidationMode.MERCHANT_ID, "desc", "by"));
        assertThrows(IllegalArgumentException.class, () -> CommercePlan.createNew("CODE", "", CommerceValidationMode.MERCHANT_ID, "desc", "by"));
    }

    @Test
    void changeStatusValidatesAllowedValues() {
        CommercePlan plan = CommercePlan.createNew("CODE", "NAME", CommerceValidationMode.MERCHANT_ID, "desc", "by");
        assertThrows(IllegalArgumentException.class, () -> plan.changeStatus("X", "actor"));

        CommercePlan updated = plan.changeStatus("I", "editor");
        assertEquals("I", updated.status());
        assertEquals("editor", updated.updatedBy());
    }

    @Test
    void updateBasicsPreservesIdentifiersAndTimestamps() {
        OffsetDateTime created = OffsetDateTime.now().minusDays(1);
        CommercePlan current = CommercePlan.rehydrate(5L, "CODE", "NAME", CommerceValidationMode.MERCHANT_ID,
                "desc", "A", created, created, "creator");

        CommercePlan updated = current.updateBasics("NEW", CommerceValidationMode.MCC.name(), "new desc", "editor");

        assertEquals(current.planId(), updated.planId());
        assertEquals(current.code(), updated.code());
        assertEquals("NEW", updated.name());
        assertEquals(CommerceValidationMode.MCC, updated.validationMode());
        assertEquals("editor", updated.updatedBy());
        assertThat(updated.updatedAt()).isAfter(created);
    }
}

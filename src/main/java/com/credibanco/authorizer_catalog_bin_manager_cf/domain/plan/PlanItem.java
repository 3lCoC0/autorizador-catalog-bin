package com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan;

public record PlanItem(
        Long planItemId,
        Long planId,
        String value,                 // MCC o MerchantId
        java.time.OffsetDateTime createdAt,
        java.time.OffsetDateTime updatedAt,
        String updatedBy
) {
    public static PlanItem rehydrate(Long id, Long pid, String val,
                                     java.time.OffsetDateTime ca,
                                     java.time.OffsetDateTime ua,
                                     String by) {
        return new PlanItem(id, pid, val, ca, ua, by);
    }
}
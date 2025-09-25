package com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan;

import java.time.OffsetDateTime;

public record CommercePlan(
        Long planId,
        String code,
        String name,
        CommerceValidationMode validationMode,
        String description,
        String status,            // 'A' | 'I'
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {
    public static CommercePlan createNew(String code, String name, CommerceValidationMode mode, String descr, String by) {
        var now = OffsetDateTime.now();
        return new CommercePlan(null, code, name, mode, descr, "A", now, now, by);
    }
    public static CommercePlan rehydrate(Long id, String code, String name, CommerceValidationMode mode, String descr,
                                         String status, OffsetDateTime createdAt, OffsetDateTime updatedAt, String updatedBy) {
        return new CommercePlan(id, code, name, mode, descr, status, createdAt, updatedAt, updatedBy);
    }
    public CommercePlan withUpdated(String newName, String newDescr, String by) {
        return new CommercePlan(planId, code, newName, validationMode, newDescr, status, createdAt, OffsetDateTime.now(), by);
    }
    public CommercePlan changeStatus(String newStatus, String by) {
        if (!"A".equals(newStatus) && !"I".equals(newStatus)) {
            throw new IllegalArgumentException("status inválido (A|I)");
        }
        return new CommercePlan(planId, code, name, validationMode, description, newStatus, createdAt, OffsetDateTime.now(), by);
    }
}

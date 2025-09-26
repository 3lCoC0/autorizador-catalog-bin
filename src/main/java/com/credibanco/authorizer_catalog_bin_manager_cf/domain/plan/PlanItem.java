package com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan;

import java.time.OffsetDateTime;

public record PlanItem(
        Long planItemId,
        Long planId,
        String value,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy,
        String status // 'A' | 'I'
) {
    public static PlanItem rehydrate(Long id, Long pid, String val,
                                     OffsetDateTime ca, OffsetDateTime ua, String by, String status) {
        return new PlanItem(id, pid, val, ca, ua, by, status);
    }
}

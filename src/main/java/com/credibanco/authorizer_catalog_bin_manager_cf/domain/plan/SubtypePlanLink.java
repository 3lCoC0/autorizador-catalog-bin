package com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan;

public record SubtypePlanLink(
        String subtypeCode,
        Long planId,
        java.time.OffsetDateTime createdAt,
        java.time.OffsetDateTime updatedAt,
        String updatedBy
) {
    public static SubtypePlanLink rehydrate(String st, Long pid,
                                            java.time.OffsetDateTime ca,
                                            java.time.OffsetDateTime ua,
                                            String by) {
        return new SubtypePlanLink(st, pid, ca, ua, by);
    }
}
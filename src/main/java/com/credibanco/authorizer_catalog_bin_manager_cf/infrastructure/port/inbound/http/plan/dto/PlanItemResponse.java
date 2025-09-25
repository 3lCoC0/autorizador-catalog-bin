package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import java.time.OffsetDateTime;

public record PlanItemResponse(
        Long planItemId,
        Long planId,
        String value,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {}

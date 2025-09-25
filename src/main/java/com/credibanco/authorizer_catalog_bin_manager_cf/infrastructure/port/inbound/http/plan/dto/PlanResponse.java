package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;

import java.time.OffsetDateTime;

public record PlanResponse(
        Long planId,
        String code,
        String name,
        CommerceValidationMode validationMode,
        String description,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {}

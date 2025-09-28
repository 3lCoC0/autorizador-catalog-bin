package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record PlanItemRequest(
        @NotBlank String planCode,
        String value,
        List<String> values,
        String updatedBy
) {}
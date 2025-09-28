package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import jakarta.validation.constraints.NotBlank;

public record PlanStatusRequest(
        @NotBlank String planCode,
        @NotBlank String status,   // 'A' | 'I'
        String updatedBy
) {}

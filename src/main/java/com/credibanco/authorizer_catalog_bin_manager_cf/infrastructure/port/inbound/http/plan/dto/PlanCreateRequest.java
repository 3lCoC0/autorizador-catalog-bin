package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlanCreateRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull CommerceValidationMode validationMode,
        String description,
        @NotBlank String updatedBy
) {}

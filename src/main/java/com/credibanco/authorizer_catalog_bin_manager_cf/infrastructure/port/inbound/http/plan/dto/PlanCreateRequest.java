package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PlanCreateRequest(
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "code no debe contener caracteres especiales") String code,
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "name no debe contener caracteres especiales") String name,
        @NotNull CommerceValidationMode validationMode,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "description no debe contener caracteres especiales") String description,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "updatedBy no debe contener caracteres especiales") String updatedBy
) {}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PlanUpdateRequest(
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "name no debe contener caracteres especiales") String name,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "description no debe contener caracteres especiales") String description,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "validationMode no debe contener caracteres especiales") String validationMode,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "updatedBy no debe contener caracteres especiales") String updatedBy
) {}
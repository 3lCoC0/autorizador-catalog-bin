package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.AlphaNumericWithSpaces;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PlanUpdateRequest(
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "name no debe contener caracteres especiales") String name,
        @AlphaNumericWithSpaces(message = "description no debe contener caracteres especiales") String description,
        @AlphaNumericWithSpaces(message = "validationMode no debe contener caracteres especiales") String validationMode,
        @AlphaNumericWithSpaces(message = "updatedBy no debe contener caracteres especiales") String updatedBy
) {}
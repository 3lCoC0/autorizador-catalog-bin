package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.AlphaNumericWithSpaces;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record PlanItemRequest(
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "planCode no debe contener caracteres especiales") String planCode,
        @AlphaNumericWithSpaces(message = "value no debe contener caracteres especiales") String value,
        List<String> values,
        @AlphaNumericWithSpaces(message = "updatedBy no debe contener caracteres especiales") String updatedBy
) {}
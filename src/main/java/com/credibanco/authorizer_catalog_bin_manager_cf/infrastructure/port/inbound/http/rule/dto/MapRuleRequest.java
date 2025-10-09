package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.AlphaNumericWithSpaces;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MapRuleRequest(
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "subtypeCode no debe contener caracteres especiales") String subtypeCode,
        @NotBlank @Pattern(regexp = "\\d{6,9}", message = "bin debe ser numérico de longitud entre 6 y 9 posiciones") String bin,
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "code no debe contener caracteres especiales") String code,
        Object value,
        @AlphaNumericWithSpaces(message = "updatedBy no debe contener caracteres especiales") String updatedBy
) {}
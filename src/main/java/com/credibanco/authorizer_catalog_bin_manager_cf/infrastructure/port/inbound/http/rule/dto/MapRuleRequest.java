package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MapRuleRequest(
        @NotBlank  @Size(min = 3, max = 3,message="subtypeCode debe ser de longitud de 3")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "name no debe contener caracteres especiales") String subtypeCode,
        @NotBlank @Pattern(regexp = "\\d{6,9}", message = "bin debe ser numérico de longitud entre 6 y 9 posiciones") String bin,
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "code no debe contener caracteres especiales") String code,
        Object value,
        String updatedBy
) {}
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ValidationUpdateRequest(
        @Size(max=200) @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "description no debe contener caracteres especiales") String description,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "valueFlag no debe contener caracteres especiales") String valueFlag,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "valueNum no debe contener caracteres especiales") Double valueNum,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "valueText no debe contener caracteres especiales")
        String valueText,
        String updatedBy
) {}

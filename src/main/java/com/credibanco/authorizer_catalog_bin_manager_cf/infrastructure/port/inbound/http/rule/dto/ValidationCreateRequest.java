package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record ValidationCreateRequest(
        @NotBlank @Size(max=40) @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "code no debe contener caracteres especiales") String code,
        @Size(max=200) @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "description no debe contener caracteres especiales") String description,
        @NotNull ValidationDataType dataType,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "valueFlag no debe contener caracteres especiales") String valueFlag,
        Double valueNum,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "valueText no debe contener caracteres especiales") String valueText,
        String createdBy
) {}
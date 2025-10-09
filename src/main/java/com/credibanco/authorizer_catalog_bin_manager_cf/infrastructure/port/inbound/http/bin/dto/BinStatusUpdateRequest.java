package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto;


import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.AlphaNumericWithSpaces;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record BinStatusUpdateRequest(
        @NotBlank
        @Pattern(regexp = "A|I", message = "status debe ser 'A' o 'I'")
        String status,
        @AlphaNumericWithSpaces(message = "updatedBy no debe contener caracteres especiales")
        String updatedBy
) {}
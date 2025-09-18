package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BinUpdateRequest(
        @NotBlank
        @Pattern(regexp = "\\d{6}|\\d{8}|\\d{9}", message = "bin debe tener 6, 8 o 9 dígitos")
        String bin,
        @NotBlank @Size(min = 3, max = 80) String name,
        @NotBlank String typeBin,
        @NotBlank String typeAccount,
        String compensationCod, // opcional
        String description,     // opcional
        @NotBlank String updatedBy
) {}
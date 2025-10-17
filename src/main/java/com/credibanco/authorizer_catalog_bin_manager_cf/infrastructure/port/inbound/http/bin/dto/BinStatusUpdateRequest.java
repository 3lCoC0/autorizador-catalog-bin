package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record BinStatusUpdateRequest(
        @Pattern(regexp = "[AI]", message = "status debe ser 'A' o 'I'")
        String status,
        String updatedBy
) {}
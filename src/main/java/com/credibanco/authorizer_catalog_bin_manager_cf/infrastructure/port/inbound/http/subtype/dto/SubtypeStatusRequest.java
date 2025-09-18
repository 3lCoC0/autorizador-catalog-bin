package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SubtypeStatusRequest(
        @NotBlank
        @Pattern(regexp = "A|I", message = "status debe ser 'A' o 'I'")
        String status,
        @NotBlank String updatedBy
) {}

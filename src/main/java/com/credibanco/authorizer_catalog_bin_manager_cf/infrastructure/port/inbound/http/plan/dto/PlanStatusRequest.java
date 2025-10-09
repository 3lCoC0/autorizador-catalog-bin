package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PlanStatusRequest(
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "planCode no debe contener caracteres especiales") String planCode,
        @NotBlank @Pattern(regexp = "A|I", message = "status debe ser 'A' o 'I'") String status,   // 'A' | 'I'
        String updatedBy
) {}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record PlanItemStatus(
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "planCode no debe contener caracteres especiales") String planCode,
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "value no debe contener caracteres especiales") String value,
        @NotBlank @Pattern(regexp = "[AI]") String status,
        String updatedBy

) {}
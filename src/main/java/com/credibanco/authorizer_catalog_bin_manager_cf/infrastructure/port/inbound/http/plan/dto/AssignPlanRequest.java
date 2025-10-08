package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AssignPlanRequest(
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "subtypeCode no debe contener caracteres especiales") String subtypeCode,
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "planCode no debe contener caracteres especiales") String planCode,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "updatedBy no debe contener caracteres especiales") String updatedBy
) {}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record PlanItemStatus(
        @NotBlank String planCode,
        @NotBlank String value,
        @NotBlank @Pattern(regexp = "A|I") String status, // para "borrar" usa I
        @NotBlank String updatedBy

) {}
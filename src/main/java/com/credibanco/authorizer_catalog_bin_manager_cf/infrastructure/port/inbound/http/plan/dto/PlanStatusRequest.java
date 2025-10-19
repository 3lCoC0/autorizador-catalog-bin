package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PlanStatusRequest(
        @NotNull(message = "status no puede ser nulo")
        @NotBlank (message = "status no puede ser vacio")
        @NotBlank @Pattern(regexp= "[AI]") String status,
        String updatedBy
) {}

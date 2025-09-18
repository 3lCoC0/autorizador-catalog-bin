package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AgencyStatusRequest(
        @NotBlank @Pattern(regexp="A|I") String status,
        @NotBlank String updatedBy
) {}

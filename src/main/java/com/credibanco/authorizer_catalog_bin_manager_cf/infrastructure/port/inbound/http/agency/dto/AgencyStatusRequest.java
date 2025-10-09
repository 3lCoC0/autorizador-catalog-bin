package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.AlphaNumericWithSpaces;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AgencyStatusRequest(
        @NotBlank @Pattern(regexp="A|I") String status,
        @AlphaNumericWithSpaces(message = "updatedBy no debe contener caracteres especiales") String updatedBy
) {}

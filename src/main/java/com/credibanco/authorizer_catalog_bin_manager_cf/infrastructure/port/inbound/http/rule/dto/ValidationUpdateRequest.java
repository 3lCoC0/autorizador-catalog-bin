package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.AlphaNumericWithSpaces;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ValidationUpdateRequest(
        @Size(max=200) @AlphaNumericWithSpaces(message = "description no debe contener caracteres especiales") String description,
        @AlphaNumericWithSpaces(message = "valueFlag no debe contener caracteres especiales") String valueFlag,
        Double valueNum,
        @AlphaNumericWithSpaces(message = "valueText no debe contener caracteres especiales") String valueText,
        @AlphaNumericWithSpaces(message = "updatedBy no debe contener caracteres especiales") String updatedBy
) {}

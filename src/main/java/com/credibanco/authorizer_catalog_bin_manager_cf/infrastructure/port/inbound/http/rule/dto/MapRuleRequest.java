package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto;

import jakarta.validation.constraints.*;

public record MapRuleRequest(
        @NotBlank String subtypeCode,
        @NotBlank String bin,
        @NotBlank String code,
        Object value,
        String updatedBy
) {}
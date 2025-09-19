package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto;

import jakarta.validation.constraints.*;

public record MapRuleRequest(
        @NotNull @Min(1) Integer priority,
        @NotBlank String updatedBy
) {}

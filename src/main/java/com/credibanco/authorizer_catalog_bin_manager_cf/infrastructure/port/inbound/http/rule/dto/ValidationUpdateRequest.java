package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ValidationUpdateRequest(
        @Size(max=200) String description,
        String valueFlag,
        Double valueNum,
        String valueText,
        String updatedBy
) {}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ValidationCreateRequest(
        @NotBlank @Size(max=40) String code,
        @Size(max=200) String description,
        @NotNull ValidationDataType dataType,
        String valueFlag,
        Double valueNum,
        String valueText,
        String createdBy
) {}
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import java.time.OffsetDateTime;

public record ValidationResponse(
        Long validationId,
        String code,
        String description,
        ValidationDataType dataType,
        String valueFlag,
        Double valueNum,
        String valueText,
        String status,
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {}

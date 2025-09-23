package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto;

import java.time.OffsetDateTime;

public record ValidationMapResponse(
        Long mapId,
        String subtypeCode,
        String bin,
        Long validationId,
        String status,
        String valueFlag,
        Double valueNum,
        String valueText,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {}

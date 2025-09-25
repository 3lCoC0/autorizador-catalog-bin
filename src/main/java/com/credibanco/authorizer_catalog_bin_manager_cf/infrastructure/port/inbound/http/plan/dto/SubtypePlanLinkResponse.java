package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import java.time.OffsetDateTime;

public record SubtypePlanLinkResponse(
        String subtypeCode,
        Long planId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {}

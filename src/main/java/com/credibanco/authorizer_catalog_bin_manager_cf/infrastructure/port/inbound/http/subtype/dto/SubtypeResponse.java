package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto;

import java.time.OffsetDateTime;

public record SubtypeResponse(
        String subtypeCode,
        String bin,
        String name,
        String description,
        String status,
        String ownerIdType,
        String ownerIdNumber,
        String binExt,
        String binEfectivo,
        Long   subtypeId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {}
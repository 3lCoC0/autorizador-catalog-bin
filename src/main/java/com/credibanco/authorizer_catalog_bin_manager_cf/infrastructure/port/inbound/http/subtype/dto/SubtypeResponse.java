package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto;

import java.time.LocalDateTime;

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
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String updatedBy
) {}
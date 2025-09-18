package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto;

public record SubtypeUpdateRequest(
        String name,
        String description,
        String ownerIdType,
        String ownerIdNumber,
        String binExt,
        String updatedBy
) {}
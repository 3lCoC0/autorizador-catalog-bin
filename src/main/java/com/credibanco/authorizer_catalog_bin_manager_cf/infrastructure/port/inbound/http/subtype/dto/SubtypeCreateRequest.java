package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto;

public record SubtypeCreateRequest(
        String subtypeCode,
        String bin,
        String name,
        String description,
        String ownerIdType,
        String ownerIdNumber,
        String binExt,
        String createdBy
) {}
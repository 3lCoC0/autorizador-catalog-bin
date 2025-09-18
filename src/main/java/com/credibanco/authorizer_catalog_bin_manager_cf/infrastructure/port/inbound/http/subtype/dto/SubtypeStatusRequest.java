package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto;

public record SubtypeStatusRequest(
        String status,   // 'A' | 'I'
        String updatedBy
) {}
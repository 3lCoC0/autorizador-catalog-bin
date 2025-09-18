package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto;

import java.time.OffsetDateTime;

public record BinResponse(
        String bin,
        String name,
        String typeBin,
        String typeAccount,
        String compensationCod,
        String description,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime  updatedAt,
        String updatedBy
) {}
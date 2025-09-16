package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.inbound.http.dto;

import java.time.LocalDateTime;

public record BinResponse(
        String bin,
        String name,
        String typeBin,
        String typeAccount,
        String compensationCod,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String updatedBy
) {}
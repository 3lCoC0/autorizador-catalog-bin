package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.inbound.http.dto;

public record BinCreateRequest(
        String bin,
        String name,
        String typeBin,
        String typeAccount,
        String compensationCod,
        String description,
        String createdBy
) {}
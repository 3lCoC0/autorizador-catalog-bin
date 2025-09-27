package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.http;

public record ApiSuccess<T>(
        String responseCode,   // "00"
        String detail,         // "Operación exitosa"
        String correlationId,  // X-Correlation-Id
        String timestamp,      // Instant/UTC
        T data                 // payload (p.ej. BinResponse o List<BinResponse>)
) {}
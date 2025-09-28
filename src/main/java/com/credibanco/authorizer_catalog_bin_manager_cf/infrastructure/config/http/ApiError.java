package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.http;

public record ApiError(
        String responseCode,  // p.ej. "03"
        String message,       // p.ej. "Ya existe un plan con ese code"
        String correlationId, // X-Correlation-Id
        String timestamp,     // Instant.now().toString()
        String path           // instance (opcional pero útil)
) {}

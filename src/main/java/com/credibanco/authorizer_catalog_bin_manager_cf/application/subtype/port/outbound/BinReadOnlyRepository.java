package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound;

import reactor.core.publisher.Mono;

public interface BinReadOnlyRepository {
    Mono<Boolean> existsById(String bin); // para validar FK y colisión BIN maestro
}
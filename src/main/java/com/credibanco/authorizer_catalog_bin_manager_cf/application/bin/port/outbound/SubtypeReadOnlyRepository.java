package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound;

import reactor.core.publisher.Mono;

public interface SubtypeReadOnlyRepository {
    Mono<Boolean> existsAnyByBin(String bin);
}
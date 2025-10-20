package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound;

import reactor.core.publisher.Mono;

public interface SubtypeReadOnlyRepository {
    Mono<Boolean> existsByCode(String subtypeCode);
    Mono<Boolean> existsByCodeAndBin(String code, String bin);
}

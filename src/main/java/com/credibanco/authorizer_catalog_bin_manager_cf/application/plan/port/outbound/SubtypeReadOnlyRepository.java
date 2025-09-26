package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound;


import reactor.core.publisher.Mono;

public interface SubtypeReadOnlyRepository {
    Mono<Boolean> existsByCode(String subtypeCode);
}
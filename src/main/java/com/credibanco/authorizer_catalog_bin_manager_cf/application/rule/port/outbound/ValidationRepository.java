package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ValidationRepository {
    Mono<Boolean> existsByCode(String code);
    Mono<Validation> save(Validation v);
    Mono<Validation> findByCode(String code);
    Mono<Validation> findById(Long id);
    Flux<Validation> findAll(String status, String search, int page, int size);
}

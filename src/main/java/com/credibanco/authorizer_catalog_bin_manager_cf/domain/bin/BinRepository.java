package com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BinRepository {
    Mono<Boolean> existsById(String bin);
    Mono<Bin> save(Bin bin);                 // upsert
    Mono<Bin> findById(String bin);
    Flux<Bin> findAll(int page, int size);
}
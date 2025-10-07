package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound;

import reactor.core.publisher.Mono;

import java.util.List;

public interface IdTypeReadOnlyRepository {
    Mono<Boolean> existsById(String idType);

    Mono<List<String>> findAllCodes();
}
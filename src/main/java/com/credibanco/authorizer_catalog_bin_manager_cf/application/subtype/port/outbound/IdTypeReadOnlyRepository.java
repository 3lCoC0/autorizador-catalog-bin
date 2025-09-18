package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound;

import reactor.core.publisher.Mono;

public interface IdTypeReadOnlyRepository {
    Mono<Boolean> existsById(String idType);
}
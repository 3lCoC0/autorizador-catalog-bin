package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound;

import reactor.core.publisher.Mono;

public interface AgencyReadOnlyRepository {
    Mono<Long> countActiveBySubtypeCode(String subtypeCode);
}
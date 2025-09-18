package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AgencyRepository {
    Mono<Boolean> existsByPk(String subtypeCode, String agencyCode);
    Mono<Agency> save(Agency aggregate); // upsert
    Mono<Agency> findByPk(String subtypeCode, String agencyCode);
    Flux<Agency> findAll(String subtypeCode, String status, String search, int page, int size);
}
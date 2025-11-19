package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SubtypeRepository {
    Mono<Boolean> existsByPk(String bin, String subtypeCode);
    Mono<Boolean> existsByBinAndExt(String bin, String binExt);
    Mono<Subtype> save(Subtype entity);
    Mono<Subtype> findByPk(String bin, String subtypeCode);
    Flux<Subtype> findAll(String binFilter, String codeFilter, String statusFilter, int page, int size);
    Mono<Boolean> existsBySubtypeCode(String subtypeCode);
}
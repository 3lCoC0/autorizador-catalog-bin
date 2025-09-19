package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ValidationMapRepository {
    Mono<Boolean> existsActive(String subtypeCode, String binEfectivo, Long validationId);
    Mono<ValidationMap> save(ValidationMap map);
    Mono<ValidationMap> findByNaturalKey(String subtypeCode, String binEfectivo, Long validationId);
    Flux<ValidationMap> findAll(String subtypeCode, String binEfectivo, String status, int page, int size);

    /** Join para resolver reglas activas y ordenadas por prioridad */
    Flux<Validation> findResolved(String subtypeCode, String binEfectivo, String status, int page, int size);
}

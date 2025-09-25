package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommercePlanRepository {
    Mono<Boolean> existsByCode(String planCode);
    Mono<CommercePlan> findByCode(String planCode);
    Flux<CommercePlan> findAll(String status, String q, int page, int size);
    Mono<CommercePlan> save(CommercePlan plan);
}

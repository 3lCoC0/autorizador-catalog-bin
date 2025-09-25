package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound;

import reactor.core.publisher.Mono;

public interface CommercePlanItemRepository {
    Flux<CommercePlanItem> findByPlan(Long planId, String q, int page, int size);
    Mono<Void> deleteByPlanAndValue(Long planId, String value, String updatedBy);
}

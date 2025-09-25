package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommercePlanItemRepository {
    Mono<PlanItem> insertMcc(Long planId, String mcc, String by);    // ← devuelve PlanItem
    Mono<Boolean> deleteByValue(Long planId, String value);          // ← opcional: true si borró
    Flux<PlanItem> listItems(Long planId, int page, int size);       // ← devuelve PlanItem, no String
}

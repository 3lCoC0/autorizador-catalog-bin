package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommercePlanItemRepository {
    Mono<PlanItem> insertMcc(Long planId, String mcc, String by);    // ← devuelve PlanItem
    Mono<PlanItem> changeStatus(Long planId, String value, String newStatus, String updatedBy);
    Flux<PlanItem> listItems(Long planId, String status, int page, int size);
    Mono<PlanItem> insertMerchant(Long planId, String merchantId, String updatedBy);
    Mono<PlanItem> findByValue(Long planId, String value);
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import reactor.core.publisher.Mono;

public interface AddPlanItemUseCase {
    Mono<PlanItem> addValue(String planCode, String value, String by);
}

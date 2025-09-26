package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import reactor.core.publisher.Flux;

public interface ListPlanItemsUseCase {
    Flux<PlanItem> list(String planCode, int page, int size,String status);
}

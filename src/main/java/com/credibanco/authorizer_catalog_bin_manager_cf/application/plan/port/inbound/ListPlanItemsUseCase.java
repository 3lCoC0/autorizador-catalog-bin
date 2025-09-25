package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlanItem;
import reactor.core.publisher.Flux;

public interface ListPlanItemsUseCase {
    Flux<CommercePlanItem> execute(String planCode, String q, int page, int size);
}

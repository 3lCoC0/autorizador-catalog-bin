package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import reactor.core.publisher.Flux;

public interface ListPlansUseCase {
    Flux<CommercePlan> execute(String status, String q, int page, int size);
}

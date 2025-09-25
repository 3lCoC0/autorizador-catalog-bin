package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import reactor.core.publisher.Mono;

public interface GetPlanUseCase {
    Mono<CommercePlan> execute(String planCode);
}
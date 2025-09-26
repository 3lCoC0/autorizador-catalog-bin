package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import reactor.core.publisher.Mono;

public interface ChangePlanItemStatusUseCase {
    Mono<PlanItem> execute(String planCode, String value, String status, String updatedBy);


    default Mono<PlanItem> inactivate(String planCode, String value, String updatedBy) {
        return execute(planCode, value, "I", updatedBy);
    }

    default Mono<PlanItem> activate(String planCode, String value, String updatedBy) {
        return execute(planCode, value, "A", updatedBy);
    }
}
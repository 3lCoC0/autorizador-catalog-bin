package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound;

import reactor.core.publisher.Mono;

public interface AddPlanItemUseCase {
    Mono<Void> addValue(String planCode, String value, String by);
}

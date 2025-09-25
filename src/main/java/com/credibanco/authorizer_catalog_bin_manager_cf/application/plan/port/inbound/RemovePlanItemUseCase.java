package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound;

import reactor.core.publisher.Mono;

public interface RemovePlanItemUseCase {
    Mono<Void> removeValue(String planCode, String value);
}

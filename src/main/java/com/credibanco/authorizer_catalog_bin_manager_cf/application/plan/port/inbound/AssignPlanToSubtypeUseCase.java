package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound;

import reactor.core.publisher.Mono;

public interface AssignPlanToSubtypeUseCase {
    Mono<Void> assign(String subtypeCode, String planCode, String by);
}

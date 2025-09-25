package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;
import reactor.core.publisher.Mono;

public interface AssignPlanToSubtypeUseCase {
    Mono<SubtypePlanLink> assign(String subtypeCode, String planCode, String by);
}

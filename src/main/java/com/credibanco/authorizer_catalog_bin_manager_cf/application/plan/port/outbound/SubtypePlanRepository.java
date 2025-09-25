package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;
import reactor.core.publisher.Mono;

public interface SubtypePlanRepository {
    Mono<SubtypePlanLink> upsert(String subtypeCode, Long planId, String updatedBy);
    Mono<SubtypePlanLink> findBySubtype(String subtypeCode);
}
// AssignPlanToSubtypeService.java
package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.AssignPlanToSubtypeUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public record AssignPlanToSubtypeService(CommercePlanRepository planRepo,
                                         SubtypePlanRepository subRepo,
                                         TransactionalOperator tx) implements AssignPlanToSubtypeUseCase {
    @Override
    public Mono<SubtypePlanLink> assign(String subtypeCode, String planCode, String by) {
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new java.util.NoSuchElementException("Plan no existe")))
                .flatMap(p -> subRepo.upsert(subtypeCode, p.planId(), by)) // ← devuelve SubtypePlanLink
                .as(tx::transactional);
    }
}

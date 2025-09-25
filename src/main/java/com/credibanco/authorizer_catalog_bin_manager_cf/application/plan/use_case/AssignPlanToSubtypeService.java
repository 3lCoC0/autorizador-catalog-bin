package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.AssignPlanToSubtypeUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.plan.R2dbcCommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.plan.R2dbcSubtypePlanRepository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public class AssignPlanToSubtypeService implements AssignPlanToSubtypeUseCase {
    private final R2dbcCommercePlanRepository planRepo;
    private final R2dbcSubtypePlanRepository subRepo;
    private final TransactionalOperator tx;
    public AssignPlanToSubtypeService(R2dbcCommercePlanRepository planRepo, R2dbcSubtypePlanRepository subRepo, TransactionalOperator tx) {
        this.planRepo = planRepo; this.subRepo = subRepo; this.tx = tx;
    }
    @Override public Mono<Void> assign(String subtypeCode, String planCode, String by) {
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new java.util.NoSuchElementException("Plan no existe")))
                .flatMap(p -> subRepo.upsert(subtypeCode, p.planId(), by))
                .then()
                .as(tx::transactional);
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.CreatePlanUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.plan.R2dbcCommercePlanRepository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public class CreatePlanService implements CreatePlanUseCase {
    private final R2dbcCommercePlanRepository repo;
    private final TransactionalOperator tx;

    public CreatePlanService(R2dbcCommercePlanRepository repo, TransactionalOperator tx) {
        this.repo = repo; this.tx = tx;
    }

    @Override public Mono<CommercePlan> execute(String code, String name,
                                                com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode mode,
                                                String description, String by) {
        return repo.existsByCode(code)
                .flatMap(exists -> exists
                        ? Mono.error(new IllegalStateException("Ya existe un plan con ese code"))
                        : repo.save(CommercePlan.createNew(code, name, mode, description, by)))
                .as(tx::transactional);
    }
}

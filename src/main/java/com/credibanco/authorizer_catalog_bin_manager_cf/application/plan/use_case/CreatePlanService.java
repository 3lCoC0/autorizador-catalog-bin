package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.CreatePlanUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public record CreatePlanService(CommercePlanRepository repo, TransactionalOperator tx)
        implements CreatePlanUseCase {
    @Override public Mono<CommercePlan> execute(String code, String name, CommerceValidationMode mode, String description, String by) {
        return repo.existsByCode(code)
                .flatMap(ex -> ex
                        ? Mono.error(new IllegalStateException("Ya existe un plan con ese code"))
                        : repo.save(CommercePlan.createNew(code, name, mode, description, by)))
                .as(tx::transactional);
    }
}
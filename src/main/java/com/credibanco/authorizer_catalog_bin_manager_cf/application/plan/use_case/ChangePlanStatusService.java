package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.ChangePlanStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import reactor.core.publisher.Mono;

public class ChangePlanStatusService implements ChangePlanStatusUseCase {
    private final CommercePlanRepository repo;

    public ChangePlanStatusService(CommercePlanRepository repo) { this.repo = repo; }

    @Override
    public Mono<CommercePlan> execute(String planCode, String status, String updatedBy) {
        if (!"A".equals(status) && !"I".equals(status)) {
            return Mono.error(new IllegalArgumentException("status inválido"));
        }
        return repo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new java.util.NoSuchElementException("Plan no encontrado")))
                .map(p -> p.changeStatus(status, updatedBy))
                .flatMap(repo::save);
    }
}

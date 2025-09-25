package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.ChangePlanStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import reactor.core.publisher.Mono;


public record ChangePlanStatusService(CommercePlanRepository repo) implements ChangePlanStatusUseCase {
    @Override
    public Mono<CommercePlan> execute(String planCode, String status, String updatedBy) {
        if (!"A".equals(status) && !"I".equals(status)) {
            return Mono.error(new IllegalArgumentException("status inválido (A|I)"));
        }
        return repo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new java.util.NoSuchElementException("Plan no encontrado")))
                .map(p -> p.changeStatus(status, updatedBy))
                .flatMap(repo::save);
    }
}
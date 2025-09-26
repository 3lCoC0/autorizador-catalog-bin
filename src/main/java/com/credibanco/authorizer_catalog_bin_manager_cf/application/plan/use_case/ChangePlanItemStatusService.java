
package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.ChangePlanItemStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record ChangePlanItemStatusService(CommercePlanRepository planRepo,
                                          CommercePlanItemRepository itemRepo,
                                          TransactionalOperator tx)
        implements ChangePlanItemStatusUseCase {

    @Override
    public Mono<PlanItem> execute(String planCode, String value, String status, String updatedBy) {
        if (!"A".equals(status) && !"I".equals(status)) {
            return Mono.error(new IllegalArgumentException("status inválido (A|I)"));
        }
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Plan no encontrado")))
                .flatMap(p -> itemRepo.changeStatus(p.planId(), value, status, updatedBy))
                .switchIfEmpty(Mono.error(new NoSuchElementException("Ítem no encontrado")))
                .as(tx::transactional);
    }
}

// AddPlanItemService.java
package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.AddPlanItemUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public record AddPlanItemService(CommercePlanRepository planRepo,
                                 CommercePlanItemRepository itemRepo,
                                 TransactionalOperator tx) implements AddPlanItemUseCase {
    @Override
    public Mono<PlanItem> addValue(String planCode, String value, String by) {
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new java.util.NoSuchElementException("Plan no existe")))
                .flatMap(p -> {
                    if (p.validationMode() == CommerceValidationMode.UNIQUE) {
                        return Mono.error(new IllegalArgumentException("El plan UNIQUE no admite ítems"));
                    }
                    if (!value.matches("^\\d{4}$")) {
                        return Mono.error(new IllegalArgumentException("Para modo MCC, 'value' debe ser 4 dígitos"));
                    }
                    return itemRepo.insertMcc(p.planId(), value, by); // ← devuelve PlanItem
                })
                .as(tx::transactional);
    }
}

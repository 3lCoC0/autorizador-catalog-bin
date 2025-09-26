// AddPlanItemService.java
package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.AddPlanItemUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public record AddPlanItemService(CommercePlanRepository planRepo,
                                 CommercePlanItemRepository itemRepo,
                                 TransactionalOperator tx) implements AddPlanItemUseCase {
    // application/plan/use_case/AddPlanItemService.java
    @Override
    public Mono<PlanItem> addValue(String planCode, String value, String by) {
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new java.util.NoSuchElementException("Plan no existe")))
                .flatMap(p -> switch (p.validationMode()) {
                    case MCC -> {
                        if (!value.matches("^\\d{4}$")) {
                            yield Mono.error(new IllegalArgumentException("Para modo MCC, 'value' debe ser 4 dígitos"));
                        }
                        yield itemRepo.insertMcc(p.planId(), value, by);
                    }
                    case MERCHANT_ID -> {
                        if (!value.matches("^\\d{9}$")) {
                            yield Mono.error(new IllegalArgumentException("Para modo MERCHANT_ID, 'value' debe ser 9 dígitos"));
                        }
                        yield itemRepo.insertMerchant(p.planId(), value, by);
                    }
                })
                .as(tx::transactional);
    }
}

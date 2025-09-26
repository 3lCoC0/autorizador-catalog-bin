package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.AddPlanItemUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record AddPlanItemService(CommercePlanRepository planRepo,
                                 CommercePlanItemRepository itemRepo,
                                 TransactionalOperator tx) implements AddPlanItemUseCase {

    @Override
    public Mono<PlanItem> addValue(String planCode, String value, String by) {
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Plan no existe")))
                .flatMap(p -> {
                    if (p.validationMode() == CommerceValidationMode.MCC) {
                        if (!value.matches("^\\d{4}$")) {
                            return Mono.error(new IllegalArgumentException("Para modo MCC, 'value' debe ser 4 dígitos"));
                        }
                    } else if (p.validationMode() == CommerceValidationMode.MERCHANT_ID) {
                        if (!value.matches("^\\d{9}$")) {
                            return Mono.error(new IllegalArgumentException("Para modo MERCHANT_ID, 'value' debe ser 9 dígitos"));
                        }
                    }

                    return itemRepo.findByValue(p.planId(), value)
                            .flatMap(existing -> Mono.<PlanItem>error(new IllegalStateException("Ítem ya existe")))
                            .switchIfEmpty(
                                    p.validationMode() == CommerceValidationMode.MCC
                                            ? itemRepo.insertMcc(p.planId(), value, by)
                                            : itemRepo.insertMerchant(p.planId(), value, by)
                            );
                })
                .as(tx::transactional)
                .onErrorMap(org.springframework.dao.DuplicateKeyException.class,
                        e -> new IllegalStateException("Ítem ya existe"));
    }

}

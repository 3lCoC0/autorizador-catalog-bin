package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.plan.R2dbcCommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.plan.R2dbcCommercePlanRepository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public class AddPlanItemService implements com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.AddPlanItemUseCase {
    private final R2dbcCommercePlanRepository planRepo;
    private final R2dbcCommercePlanItemRepository itemRepo;
    private final TransactionalOperator tx;

    public AddPlanItemService(R2dbcCommercePlanRepository planRepo, R2dbcCommercePlanItemRepository itemRepo, TransactionalOperator tx) {
        this.planRepo = planRepo; this.itemRepo = itemRepo; this.tx = tx;
    }

    @Override
    public Mono<Void> addValue(String planCode, String value, String by) {
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new java.util.NoSuchElementException("Plan no existe")))
                .flatMap(p -> {
                    if (p.validationMode() == CommerceValidationMode.UNIQUE) {
                        return Mono.error(new IllegalArgumentException("El plan UNIQUE no admite ítems"));
                    }
                    // Modo MCC: validar 4 dígitos
                    if (!value.matches("^\\d{4}$")) {
                        return Mono.error(new IllegalArgumentException("Para modo MCC, 'value' debe ser 4 dígitos"));
                    }
                    return itemRepo.insertMcc(p.planId(), value, by).then();
                }).as(tx::transactional);
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.RemovePlanItemUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import reactor.core.publisher.Mono;

public class RemovePlanItemService implements RemovePlanItemUseCase {
    private final CommercePlanRepository planRepo;
    private final CommercePlanItemRepository itemRepo;

    public RemovePlanItemService(CommercePlanRepository planRepo, CommercePlanItemRepository itemRepo) {
        this.planRepo = planRepo;
        this.itemRepo = itemRepo;
    }

    @Override
    public Mono<Void> execute(String planCode, String value, String updatedBy) {
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new java.util.NoSuchElementException("Plan no encontrado")))
                .flatMap(p -> itemRepo.deleteByPlanAndValue(p.planId(), value, updatedBy))
                .then();
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.RemovePlanItemUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import reactor.core.publisher.Mono;


public record RemovePlanItemService(CommercePlanRepository planRepo,
                                    CommercePlanItemRepository itemRepo)
        implements RemovePlanItemUseCase {
    @Override
    public Mono<Void> removeValue(String planCode, String value) {
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new java.util.NoSuchElementException("Plan no encontrado")))
                .flatMap(p -> itemRepo.deleteByValue(p.planId(), value))
                .then();
    }
}

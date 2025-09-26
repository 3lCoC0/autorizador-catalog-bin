package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.ListPlanItemsUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record ListPlanItemsService(CommercePlanRepository planRepo,
                                   CommercePlanItemRepository itemRepo)
        implements ListPlanItemsUseCase {

    @Override
    public Flux<PlanItem> list(String planCode, int page, int size, String status) {
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new java.util.NoSuchElementException("Plan no encontrado")))
                .flatMapMany(p -> itemRepo.listItems(p.planId(), status, page, size));
    }
}
package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.ListPlanItemsUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlanItem;
import reactor.core.publisher.Flux;

public class ListPlanItemsService implements ListPlanItemsUseCase {
    private final CommercePlanRepository planRepo;
    private final CommercePlanItemRepository itemRepo;

    public ListPlanItemsService(CommercePlanRepository planRepo, CommercePlanItemRepository itemRepo) {
        this.planRepo = planRepo;
        this.itemRepo = itemRepo;
    }

    @Override
    public Flux<CommercePlanItem> execute(String planCode, String q, int page, int size) {
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Flux.<CommercePlanItem>error(new java.util.NoSuchElementException("Plan no encontrado")))
                .flatMapMany(p -> itemRepo.findByPlan(p.planId(), q, page, size));
    }
}

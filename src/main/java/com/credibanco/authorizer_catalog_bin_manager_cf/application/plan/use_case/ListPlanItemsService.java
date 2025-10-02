package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.ListPlanItemsUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public record ListPlanItemsService(CommercePlanRepository planRepo,
                                   CommercePlanItemRepository itemRepo)
        implements ListPlanItemsUseCase {

    @Override
    public Flux<PlanItem> list(String planCode, int page, int size, String status) {
        log.info("ListPlanItemsService IN code={} status={} page={} size={}", planCode, status, page, size);
        if (page < 0 || size <= 0) {
            return Flux.error(new AppException(AppError.PLAN_ITEM_INVALID_DATA, "page>=0 y size>0"));
        }
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.<com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan>error(
                        new AppException(AppError.PLAN_NOT_FOUND)))
                .flatMapMany(p -> itemRepo.listItems(p.planId(), status, page, size))
                .doOnComplete(() -> log.info("ListPlanItemsService OK code={}", planCode));
    }
}

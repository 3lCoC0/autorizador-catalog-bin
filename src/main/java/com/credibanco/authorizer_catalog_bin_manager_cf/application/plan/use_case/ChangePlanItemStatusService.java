package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.ChangePlanItemStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Slf4j
public record ChangePlanItemStatusService(CommercePlanRepository planRepo,
                                          CommercePlanItemRepository itemRepo,
                                          TransactionalOperator tx)
        implements ChangePlanItemStatusUseCase {

    @Override
    public Mono<PlanItem> execute(String planCode, String value, String status, String updatedBy) {
        log.info("ChangePlanItemStatusService IN planCode={} value={} status={} by={}", planCode, value, status, updatedBy);
        if (!"A".equals(status) && !"I".equals(status)) {
            return Mono.<PlanItem>error(new AppException(AppError.PLAN_ITEM_INVALID_DATA, "status inválido (A|I)"));
        }
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.<com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan>error(
                        new AppException(AppError.PLAN_NOT_FOUND)))
                .flatMap(p -> itemRepo.changeStatus(p.planId(), value, status, updatedBy))
                .switchIfEmpty(Mono.<PlanItem>error(new AppException(AppError.PLAN_ITEM_NOT_FOUND,
                        "value=" + value + " en plan " + planCode)))
                .doOnSuccess(pi -> log.info("ChangePlanItemStatusService OK planId={} itemId={} status={}",
                        pi.planId(), pi.planItemId(), status))
                .as(tx::transactional);
    }
}

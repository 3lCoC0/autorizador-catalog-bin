package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.ChangePlanStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public record ChangePlanStatusService(CommercePlanRepository repo) implements ChangePlanStatusUseCase {
    @Override
    public Mono<CommercePlan> execute(String planCode, String status, String updatedBy) {
        log.info("ChangePlanStatusService IN code={} status={} by={}", planCode, status, updatedBy);
        if (!"A".equals(status) && !"I".equals(status)) {
            return Mono.<CommercePlan>error(new AppException(AppError.PLAN_INVALID_DATA, "status inválido (A|I)"));
        }
        return repo.findByCode(planCode)
                .switchIfEmpty(Mono.<CommercePlan>error(new AppException(AppError.PLAN_NOT_FOUND)))
                .map(p -> {
                    try { return p.changeStatus(status, updatedBy); }
                    catch (IllegalArgumentException iae) {
                        throw new AppException(AppError.PLAN_INVALID_DATA, iae.getMessage());
                    }
                })
                .flatMap(repo::save)
                .doOnSuccess(p -> log.info("ChangePlanStatusService OK code={} newStatus={}", p.code(), p.status()));
    }
}

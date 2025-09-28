package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.GetPlanUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Slf4j
public record GetPlanService(CommercePlanRepository repo) implements GetPlanUseCase {
    @Override public Mono<CommercePlan> execute(String planCode) {
        log.info("GetPlanService IN code={}", planCode);
        return repo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Plan no encontrado")))
                .doOnSuccess(p -> log.info("GetPlanService OK code={} id={}", p.code(), p.planId()));
    }
}

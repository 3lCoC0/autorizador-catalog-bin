package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.UpdatePlanUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;
import java.util.Objects;

@Slf4j
public record UpdatePlanService(CommercePlanRepository repo) implements UpdatePlanUseCase {
    @Override
    public Mono<CommercePlan> execute(String planCode, String planName, String description,
                                      String validationMode /* puede ser null */, String updatedBy) {
        log.info("UpdatePlanService IN code={} newName={} mode={} by={}", planCode, planName, validationMode, updatedBy);

        return repo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Plan no encontrado")))
                .map(p -> {
                    String newMode = validationMode == null ? null : validationMode.trim().toUpperCase();
                    if (newMode != null &&
                            !Objects.equals(newMode, "MERCHANT_ID") &&
                            !Objects.equals(newMode, "MCC")) {
                        throw new IllegalArgumentException("validationMode inválido (MERCHANT_ID | MCC)");
                    }
                    return p.updateBasics(planName, newMode, description, updatedBy);
                })
                .flatMap(repo::save)
                .doOnSuccess(p -> log.info("UpdatePlanService OK code={} id={}", p.code(), p.planId()));
    }
}

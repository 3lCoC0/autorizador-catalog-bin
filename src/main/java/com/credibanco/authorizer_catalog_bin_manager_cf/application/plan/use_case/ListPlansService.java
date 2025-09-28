package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.ListPlansUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public record ListPlansService(CommercePlanRepository repo) implements ListPlansUseCase {
    @Override public Flux<CommercePlan> execute(String status, String q, int page, int size) {
        log.info("ListPlansService IN status={} q={} page={} size={}", status, q, page, size);
        return repo.findAll(status, q, page, size)
                .doOnComplete(() -> log.info("ListPlansService OK status={} q={}", status, q));
    }
}

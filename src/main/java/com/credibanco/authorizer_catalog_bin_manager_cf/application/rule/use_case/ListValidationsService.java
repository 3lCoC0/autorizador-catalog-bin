package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.ListValidationsUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public record ListValidationsService(ValidationRepository repo) implements ListValidationsUseCase {
    @Override
    public Flux<Validation> execute(String status, String search, int page, int size) {
        long t0 = System.nanoTime();
        log.info("UC:Validation:List:start status={} q={} page={} size={}", status, search, page, size);
        return repo.findAll(status, search, page, size)
                .doOnComplete(() -> log.info("UC:Validation:List:done elapsedMs={}",
                        (System.nanoTime()-t0)/1_000_000));
    }
}

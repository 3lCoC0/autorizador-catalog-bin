package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.GetValidationUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Slf4j
public record GetValidationService(ValidationRepository repo) implements GetValidationUseCase {
    @Override
    public Mono<Validation> execute(String code) {
        long t0 = System.nanoTime();
        log.debug("UC:Validation:Get:start code={}", code);
        return repo.findByCode(code)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Validation no encontrada")))
                .doOnSuccess(v -> log.info("UC:Validation:Get:done code={} elapsedMs={}", v.code(),
                        (System.nanoTime()-t0)/1_000_000));
    }
}

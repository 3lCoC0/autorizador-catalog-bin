package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.UpdateValidationUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Slf4j
public record UpdateValidationService(ValidationRepository repo) implements UpdateValidationUseCase {
    private static long ms(long t0) { return (System.nanoTime()-t0)/1_000_000; }

    @Override
    public Mono<Validation> execute(String code, String description, String byNullable) {
        long t0 = System.nanoTime();
        log.debug("UC:Validation:Update:start code={}", code);
        return repo.findByCode(code)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Validation no encontrada")))
                .map(cur -> cur.updateBasics(description, byNullable))
                .flatMap(repo::save)
                .doOnSuccess(v -> log.info("UC:Validation:Update:done code={} elapsedMs={}", v.code(), ms(t0)));
    }
}

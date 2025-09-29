package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.UpdateValidationUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public record UpdateValidationService(ValidationRepository repo) implements UpdateValidationUseCase {
    private static long ms(long t0) { return (System.nanoTime()-t0)/1_000_000; }

    @Override
    public Mono<Validation> execute(String code, String description, String byNullable) {
        long t0 = System.nanoTime();
        return repo.findByCode(code)
                .switchIfEmpty(Mono.error(new AppException(AppError.RULES_VALIDATION_NOT_FOUND)))
                .map(cur -> {
                    try { return cur.updateBasics(description, byNullable); }
                    catch (IllegalArgumentException iae) {
                        throw new AppException(AppError.RULES_VALIDATION_INVALID_DATA, iae.getMessage());
                    }
                })
                .flatMap(repo::save)
                .doOnSuccess(v -> log.info("UC:Validation:Update:done code={} elapsedMs={}", v.code(), ms(t0)));
    }
}

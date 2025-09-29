package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.GetValidationUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public record GetValidationService(ValidationRepository repo) implements GetValidationUseCase {
    @Override
    public Mono<Validation> execute(String code) {
        long t0 = System.nanoTime();
        return repo.findByCode(code)
                .switchIfEmpty(Mono.error(new AppException(AppError.RULES_VALIDATION_NOT_FOUND)))
                .doOnSuccess(v -> log.info("UC:Validation:Get:done code={} elapsedMs={}", v.code(),
                        (System.nanoTime()-t0)/1_000_000));
    }
}

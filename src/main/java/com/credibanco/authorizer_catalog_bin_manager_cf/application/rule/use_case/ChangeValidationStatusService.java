package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.ChangeValidationStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Slf4j
public record ChangeValidationStatusService(ValidationRepository repo, TransactionalOperator tx)
        implements ChangeValidationStatusUseCase {

    private static long ms(long t0) { return (System.nanoTime()-t0)/1_000_000; }

    @Override
    public Mono<Validation> execute(String code, String newStatus, String byNullable) {
        long t0 = System.nanoTime();
        if (!"A".equals(newStatus) && !"I".equals(newStatus)) {
            return Mono.error(new AppException(AppError.RULES_VALIDATION_INVALID_DATA, "status debe ser 'A' o 'I'"));
        }
        return repo.findByCode(code)
                .switchIfEmpty(Mono.error(new AppException(AppError.RULES_VALIDATION_NOT_FOUND, "code=" + code)))
                .map(cur -> {
                    try { return cur.changeStatus(newStatus, byNullable); }
                    catch (IllegalArgumentException iae) {
                        throw new AppException(AppError.RULES_VALIDATION_INVALID_DATA, iae.getMessage());
                    }
                })
                .flatMap(repo::save)
                .doOnSuccess(v -> { long e=(System.nanoTime()-t0)/1_000_000;
                    log.info("UC:Validation:ChangeStatus:done code={} status={} elapsedMs={}", v.code(), v.status(), e);
                })
                .as(tx::transactional);
    }
}

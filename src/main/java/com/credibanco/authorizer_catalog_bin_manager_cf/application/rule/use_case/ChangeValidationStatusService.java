package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.ChangeValidationStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Slf4j
public record ChangeValidationStatusService(ValidationRepository repo, TransactionalOperator tx)
        implements ChangeValidationStatusUseCase {
    private static long ms(long t0) { return (System.nanoTime()-t0)/1_000_000; }

    @Override
    public Mono<Validation> execute(String code, String newStatus, String byNullable) {
        long t0 = System.nanoTime();
        log.debug("UC:Validation:ChangeStatus:start code={} newStatus={}", code, newStatus);
        if (!"A".equals(newStatus) && !"I".equals(newStatus))
            return Mono.error(new IllegalArgumentException("status inválido"));

        return repo.findByCode(code)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Validation no encontrada")))
                .map(cur -> cur.changeStatus(newStatus, byNullable))
                .flatMap(repo::save)
                .doOnSuccess(v -> log.info("UC:Validation:ChangeStatus:done code={} status={} elapsedMs={}",
                        v.code(), v.status(), ms(t0)))
                .as(tx::transactional);
    }
}

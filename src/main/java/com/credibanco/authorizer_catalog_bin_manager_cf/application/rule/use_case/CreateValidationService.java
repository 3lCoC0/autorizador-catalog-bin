package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.CreateValidationUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Slf4j
public record CreateValidationService(ValidationRepository repo, TransactionalOperator tx)
        implements CreateValidationUseCase {

    private static long ms(long t0) { return (System.nanoTime()-t0)/1_000_000; }

    @Override
    public Mono<Validation> execute(String code, String description, ValidationDataType type, String createdByNullable) {
        long t0 = System.nanoTime();
        log.debug("UC:Validation:Create:start code={} type={}", code, type);
        return repo.existsByCode(code)
                .flatMap(exists -> exists
                        ? Mono.error(new IllegalStateException("Validation ya existe"))
                        : repo.save(Validation.createNew(code, description, type, createdByNullable)))
                .doOnSuccess(v -> log.info("UC:Validation:Create:done code={} elapsedMs={}", code, ms(t0)))
                .as(tx::transactional);
    }
}

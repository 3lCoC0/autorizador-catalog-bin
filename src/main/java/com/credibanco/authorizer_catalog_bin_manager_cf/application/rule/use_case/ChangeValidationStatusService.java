package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.ChangeValidationStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record ChangeValidationStatusService(ValidationRepository repo, TransactionalOperator tx)
        implements ChangeValidationStatusUseCase {
    @Override public Mono<Validation> execute(String code, String newStatus) {
        if (!"A".equals(newStatus) && !"I".equals(newStatus))
            return Mono.error(new IllegalArgumentException("status inválido"));
        return repo.findByCode(code)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Validation no encontrada")))
                .map(v -> v.changeStatus(newStatus))
                .flatMap(repo::save)
                .as(tx::transactional);
    }
}

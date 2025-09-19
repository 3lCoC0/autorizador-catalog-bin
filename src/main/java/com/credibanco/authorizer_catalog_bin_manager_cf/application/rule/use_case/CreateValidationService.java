package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.CreateValidationUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public record CreateValidationService(ValidationRepository repo, TransactionalOperator tx)
        implements CreateValidationUseCase {
    @Override
    public Mono<Validation> execute(String code, String description, ValidationDataType type,
                                    String valueFlag, Double valueNum, String valueText) {
        var draft = Validation.createNew(code, description, type, valueFlag, valueNum, valueText, "system");
        return repo.existsByCode(code)
                .flatMap(exists -> exists
                        ? Mono.error(new IllegalStateException("Validation code ya existe"))
                        : repo.save(draft))
                .as(tx::transactional);
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.GetValidationUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record GetValidationService(ValidationRepository repo) implements GetValidationUseCase {
    @Override public Mono<Validation> execute(String code) {
        return repo.findByCode(code)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Validation no encontrada")));
    }
}

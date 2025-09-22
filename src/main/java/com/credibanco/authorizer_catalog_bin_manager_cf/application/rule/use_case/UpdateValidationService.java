package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.UpdateValidationUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record UpdateValidationService(ValidationRepository repo) implements UpdateValidationUseCase {
    @Override
    public Mono<Validation> execute(String code, String description, String flag, Double num, String text,String updatedBy) {
        return repo.findByCode(code)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Validation no encontrada")))
                .map(v -> v.updateBasics(description, flag, num, text,updatedBy))
                .flatMap(repo::save);
    }
}

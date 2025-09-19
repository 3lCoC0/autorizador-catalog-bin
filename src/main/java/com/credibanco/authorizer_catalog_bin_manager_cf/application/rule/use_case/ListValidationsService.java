package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.ListValidationsUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import reactor.core.publisher.Flux;

public record ListValidationsService(ValidationRepository repo) implements ListValidationsUseCase {
    @Override public Flux<Validation> execute(String status, String search, int page, int size) {
        return repo.findAll(status, search, page, size);
    }
}

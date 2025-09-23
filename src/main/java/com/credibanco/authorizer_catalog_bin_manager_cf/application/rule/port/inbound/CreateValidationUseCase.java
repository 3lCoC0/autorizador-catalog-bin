package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import reactor.core.publisher.Mono;

public interface CreateValidationUseCase {
    Mono<Validation> execute(String code, String description, ValidationDataType type);
}

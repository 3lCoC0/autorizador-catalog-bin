package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import reactor.core.publisher.Mono;

public interface UpdateValidationUseCase {
    Mono<Validation> execute(String code, String description, String valueFlag, Double valueNum, String valueText);
}

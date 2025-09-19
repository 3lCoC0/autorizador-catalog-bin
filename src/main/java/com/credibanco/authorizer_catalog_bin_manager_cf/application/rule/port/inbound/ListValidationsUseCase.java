package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import reactor.core.publisher.Flux;

public interface ListValidationsUseCase {
    Flux<Validation> execute(String status, String search, int page, int size);
    default Flux<Validation> execute() { return execute(null, null, 0, 20); }
}

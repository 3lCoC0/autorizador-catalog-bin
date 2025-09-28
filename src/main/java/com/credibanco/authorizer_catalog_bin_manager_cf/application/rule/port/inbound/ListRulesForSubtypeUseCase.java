package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import reactor.core.publisher.Flux;

public interface ListRulesForSubtypeUseCase {
    Flux<ValidationMap> execute(String subtypeCode, String bin, String status, int page, int size);
    default Flux<ValidationMap> execute(String subtypeCode, String bin) {
        return execute(subtypeCode, bin, "A", 0, 100);
    }

    default Flux<ValidationMap> execute(String subtypeCode, String status, int page, int size) {
        return execute(subtypeCode, null, status, page, size);
    }
}
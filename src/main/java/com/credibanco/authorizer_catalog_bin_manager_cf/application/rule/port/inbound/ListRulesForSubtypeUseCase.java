package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import reactor.core.publisher.Flux;

public interface ListRulesForSubtypeUseCase {
    Flux<Validation> execute(String subtypeCode, String binEfectivo, String onlyStatus /* A|I|null */, int page, int size);
    default Flux<Validation> execute(String subtypeCode, String binEfectivo) {
        return execute(subtypeCode, binEfectivo, "A", 0, 100);
    }
}

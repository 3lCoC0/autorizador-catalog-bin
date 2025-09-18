package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import reactor.core.publisher.Flux;

public interface ListAgenciesUseCase {
    Flux<Agency> execute(String subtypeCode, String status, String search, int page, int size);
    default Flux<Agency> execute(String subtypeCode, String status, String search) {
        return execute(subtypeCode, status, search, 0, 20);
    }
}

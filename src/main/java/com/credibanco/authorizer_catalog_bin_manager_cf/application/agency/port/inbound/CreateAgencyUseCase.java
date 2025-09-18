package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import reactor.core.publisher.Mono;

public interface CreateAgencyUseCase {
    Mono<Agency> execute(Agency draft);
}

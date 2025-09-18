package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import reactor.core.publisher.Mono;

public interface GetSubtypeUseCase {
    Mono<Subtype> execute(String bin, String subtypeCode);
}

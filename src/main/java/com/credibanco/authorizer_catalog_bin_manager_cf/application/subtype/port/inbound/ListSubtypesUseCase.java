package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import reactor.core.publisher.Flux;

public interface ListSubtypesUseCase {
    Flux<Subtype> execute(String bin, String code, String status, int page, int size);
}

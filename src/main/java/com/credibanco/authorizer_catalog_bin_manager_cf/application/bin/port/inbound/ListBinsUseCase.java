package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import reactor.core.publisher.Flux;

public interface ListBinsUseCase {
    Flux<Bin> execute(int page, int size);

       default Flux<Bin> execute() {
        return execute(0, 20);
    }
}

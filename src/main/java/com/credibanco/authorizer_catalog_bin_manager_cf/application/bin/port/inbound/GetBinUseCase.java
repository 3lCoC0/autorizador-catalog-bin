package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import reactor.core.publisher.Mono;

public interface GetBinUseCase {
    Mono<Bin> execute(String bin);
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import reactor.core.publisher.Mono;

public interface ChangeBinStatusUseCase {
    Mono<Bin> execute(String bin, String newStatus, String updatedBy);
}

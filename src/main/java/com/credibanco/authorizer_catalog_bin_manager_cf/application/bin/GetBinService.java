package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.BinRepository;
import reactor.core.publisher.Mono;

public class GetBinService implements GetBinUseCase {
    private final BinRepository repo;
    public GetBinService(BinRepository repo) { this.repo = repo; }

    @Override
    public Mono<Bin> execute(String bin) {
        return repo.findById(bin)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("BIN no encontrado")));
    }
}

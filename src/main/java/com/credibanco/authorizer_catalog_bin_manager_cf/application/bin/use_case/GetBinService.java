package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.GetBinUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public class GetBinService implements GetBinUseCase {
    private final BinRepository repo;
    public GetBinService(BinRepository repo) { this.repo = repo; }

    @Override
    public Mono<Bin> execute(String bin) {
        return repo.findById(bin)
                .switchIfEmpty(Mono.error(new NoSuchElementException("BIN no encontrado")));
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.BinRepository;
import reactor.core.publisher.Flux;

public class ListBinsService implements ListBinsUseCase {
    private final BinRepository repo;
    public ListBinsService(BinRepository repo) { this.repo = repo; }

    @Override
    public Flux<Bin> execute(int page, int size) {
        return repo.findAll(page, size);
    }
}

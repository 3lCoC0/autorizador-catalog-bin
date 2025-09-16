package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin;


import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.BinRepository;
import reactor.core.publisher.Flux;

public class ListBinsUseCase {
    private final BinRepository repo;

    public ListBinsUseCase(BinRepository repo) { this.repo = repo; }

    public Flux<Bin> execute(int page, int size) { return repo.findAll(page, size); }
}
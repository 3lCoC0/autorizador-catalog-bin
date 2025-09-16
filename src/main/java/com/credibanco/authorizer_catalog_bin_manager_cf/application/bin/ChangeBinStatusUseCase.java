package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin;


import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.BinRepository;
import reactor.core.publisher.Mono;

public class ChangeBinStatusUseCase {
    private final BinRepository repo;

    public ChangeBinStatusUseCase(BinRepository repo) { this.repo = repo; }

    public Mono<Bin> execute(String bin, String newStatus, String updatedBy) {
        if (!newStatus.matches("[AI]")) return Mono.error(new IllegalArgumentException("status inválido"));

        return repo.findById(bin)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("BIN no encontrado")))
                .map(b -> b.withStatus(newStatus, updatedBy))
                .flatMap(repo::save);
    }
}
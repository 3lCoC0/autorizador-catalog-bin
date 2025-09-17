package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.BinRepository;
import reactor.core.publisher.Mono;

public class ChangeBinStatusService implements ChangeBinStatusUseCase {
    private final BinRepository repo;
    public ChangeBinStatusService(BinRepository repo) { this.repo = repo; }

    @Override
    public Mono<Bin> execute(String bin, String newStatus, String updatedBy) {
        return repo.findById(bin)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("BIN no encontrado")))
                .map(b -> b.changeStatus(newStatus, updatedBy)) // usa tu método de dominio correcto
                .flatMap(repo::save);
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.BinRepository;
import reactor.core.publisher.Mono;

public class CreateBinService implements CreateBinUseCase {
    private final BinRepository repo;
    public CreateBinService(BinRepository repo) { this.repo = repo; }

    @Override
    public Mono<Bin> execute(String bin, String name, String typeBin, String typeAccount,
                             String compensationCod, String description, String createdBy) {
        if (bin == null || bin.length() < 6) return Mono.error(new IllegalArgumentException("BIN inválido"));
        return repo.existsById(bin)
                .flatMap(exists -> exists
                        ? Mono.error(new IllegalStateException("El BIN ya existe"))
                        : repo.save(Bin.createNew(bin, name, typeBin, typeAccount, compensationCod, description, createdBy)));
    }
}

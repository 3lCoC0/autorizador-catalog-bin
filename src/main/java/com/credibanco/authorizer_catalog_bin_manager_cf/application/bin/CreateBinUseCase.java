package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.BinRepository;
import reactor.core.publisher.Mono;


public class CreateBinUseCase {
    private final BinRepository repo;
    public CreateBinUseCase(BinRepository repo) { this.repo = repo; }

    public Mono<Bin> execute(String bin, String name, String typeBin, String typeAccount,
                             String compensationCod, String description, String createdBy) {
        var entity = Bin.createNew(bin, name, typeBin, typeAccount, compensationCod, description, createdBy);
        return repo.existsById(bin)
                .flatMap(exists -> exists
                        ? Mono.error(new IllegalStateException("El BIN ya existe"))
                        : repo.save(entity));
    }
}
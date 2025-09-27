package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;


import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.UpdateBinUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;


public record UpdateBinService(BinRepository repo, TransactionalOperator tx)
        implements UpdateBinUseCase {
    @Override
    public Mono<Bin> execute(String bin, String name, String typeBin, String typeAccount,
                             String compensationCod, String description,
                             String usesBinExt, Integer binExtDigits,
                             String updatedByNullable) {
        return repo.findById(bin)
                .switchIfEmpty(Mono.error(new NoSuchElementException("BIN no existe")))
                .flatMap(current -> repo.save(
                        current.updateBasics(name, typeBin, typeAccount, compensationCod, description,
                                usesBinExt, binExtDigits, updatedByNullable)
                ))
                .as(tx::transactional);
    }
}
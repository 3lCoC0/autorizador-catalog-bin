package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;


import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.UpdateBinUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

/**
 * PUT: reemplazo completo de atributos editables.
 * Garantiza 404 si el BIN no existe.
 */
public record UpdateBinService(BinRepository repo, TransactionalOperator tx)
        implements UpdateBinUseCase {

    @Override
    public Mono<Bin> execute(String bin, String name, String typeBin, String typeAccount,
                             String compensationCod, String description, String updatedBy) {

        return repo.findById(bin)
                .switchIfEmpty(Mono.error(new NoSuchElementException("BIN no existe")))
                .flatMap(current -> {
                    // Sugerido: método de dominio que aplica invariantes de actualización.
                    // Si aún no lo tienes, reemplaza por la fábrica que uses.
                    Bin updated = current.updateBasics(name, typeBin, typeAccount, compensationCod, description, updatedBy);
                    return repo.save(updated);
                })
                .as(tx::transactional);
    }
}
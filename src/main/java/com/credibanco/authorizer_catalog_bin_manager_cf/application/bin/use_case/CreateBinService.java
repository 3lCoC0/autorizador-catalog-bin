package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.CreateBinUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;


public record CreateBinService(BinRepository repo, TransactionalOperator tx)
        implements CreateBinUseCase {

    @Override
    public Mono<Bin> execute(String bin, String name, String typeBin, String typeAccount,
                             String compensationCod, String description, String createdBy) {

        return Mono.defer(() -> {
            // Validación rápida aquí; el resto déjalo a la fábrica de dominio
            if (bin == null || bin.length() < 6) {
                return Mono.error(new IllegalArgumentException("BIN inválido"));
            }

            // Fábrica de dominio (aplica invariantes del agregado)
            Bin aggregate = Bin.createNew(bin, name, typeBin, typeAccount, compensationCod, description, createdBy);

            // Idempotencia básica por clave natural (BIN)
            return repo.existsById(bin)
                    .flatMap(exists -> exists
                            ? Mono.error(new IllegalStateException("El BIN ya existe"))
                            : repo.save(aggregate))
                    // MERGE + SELECT quedan en UNA transacción
                    .as(tx::transactional);
        });
    }
}

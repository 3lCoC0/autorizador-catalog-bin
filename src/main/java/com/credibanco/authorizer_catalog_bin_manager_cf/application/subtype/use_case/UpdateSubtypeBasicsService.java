package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.UpdateSubtypeBasicsUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record UpdateSubtypeBasicsService(
        SubtypeRepository repo,
        TransactionalOperator tx
) implements UpdateSubtypeBasicsUseCase {

    @Override
    public Mono<Subtype> execute(String bin, String subtypeCode,
                                 String name, String descripcion,
                                 String ownerIdType, String ownerIdNumber,
                                 String newBinExt, String updatedBy) {

        return repo.findByPk(bin, subtypeCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("SUBTYPE no encontrado")))
                .flatMap(current -> {
                    Subtype updated = current.updateBasics(name, descripcion, ownerIdType, ownerIdNumber, newBinExt, updatedBy);

                    boolean extChanged = (current.binExt() == null && updated.binExt() != null)
                            || (current.binExt() != null && !current.binExt().equals(updated.binExt()));

                    Mono<Boolean> extExists = extChanged && updated.binExt() != null
                            ? repo.existsByBinAndExt(updated.bin(), updated.binExt())
                            : Mono.just(false);

                    return extExists.flatMap(exists -> exists
                            ? Mono.error(new IllegalStateException("bin_ext ya usado para ese BIN"))
                            : repo.save(updated));
                })
                .as(tx::transactional);
    }
}

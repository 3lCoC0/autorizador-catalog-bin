package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.IdTypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public interface CreateSubtypeUseCase {
    Mono<Subtype> execute(String subtypeCode, String bin, String name, String descripcion,
                          String ownerIdType, String ownerIdNumber, String binExt, String createdBy);
}

public record CreateSubtypeService(
        SubtypeRepository repo,
        BinReadOnlyRepository binRepo,
        IdTypeReadOnlyRepository idTypeRepo,
        TransactionalOperator tx
) implements CreateSubtypeUseCase {

    @Override
    public Mono<Subtype> execute(String subtypeCode, String bin, String name, String descripcion,
                                 String ownerIdType, String ownerIdNumber, String binExt, String createdBy) {

        // Ensambla entidad en memoria (aplica normalización/6+3-8+1-9+0 y calcula BIN_EFECTIVO)
        Subtype draft = Subtype.createNew(subtypeCode, bin, name, descripcion,
                        ownerIdType, ownerIdNumber, binExt, createdBy)
                .changeStatus("I", createdBy); // crear en I

        Mono<Boolean> fkBin        = binRepo.existsById(draft.bin());
        Mono<Boolean> bin9Collision= (draft.bin().length()==6 || draft.bin().length()==8)
                ? binRepo.existsById(draft.binEfectivo())
                : Mono.just(false);
        Mono<Boolean> fkIdType     = draft.ownerIdType()==null ? Mono.just(true) : idTypeRepo.existsById(draft.ownerIdType());
        Mono<Boolean> pkExists     = repo.existsByPk(draft.bin(), draft.subtypeCode());
        Mono<Boolean> extExists    = draft.binExt()==null ? Mono.just(false) : repo.existsByBinAndExt(draft.bin(), draft.binExt());

        return Mono.zip(fkBin, bin9Collision, fkIdType, pkExists, extExists)
                .flatMap(t -> {
                    if (!t.getT1()) return Mono.error(new IllegalArgumentException("BIN no existe (FK)"));
                    if (t.getT2())  return Mono.error(new IllegalStateException(
                            "Colisión: ya existe BIN maestro " + draft.binEfectivo() + " (use ese BIN9)"));
                    if (!t.getT3()) return Mono.error(new IllegalArgumentException("OWNER_ID_TYPE no existe"));
                    if (t.getT4())  return Mono.error(new IllegalStateException("Ya existe SUBTYPE para ese BIN y código"));
                    if (t.getT5())  return Mono.error(new IllegalStateException("bin_ext ya usado para ese BIN"));
                    return repo.save(draft);
                })
                .as(tx::transactional);
    }
}
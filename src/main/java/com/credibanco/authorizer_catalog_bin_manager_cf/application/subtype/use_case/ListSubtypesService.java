package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.ListSubtypesUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import reactor.core.publisher.Flux;

import java.util.NoSuchElementException;

public record ListSubtypesService(
        SubtypeRepository repo,
        BinReadOnlyRepository binRepo
) implements ListSubtypesUseCase {

    @Override
    public Flux<Subtype> execute(String bin, String code, String status, int page, int size) {
        // Si el cliente filtra por BIN, primero verificamos que ese BIN exista en la tabla BIN.
        if (bin != null && !bin.isBlank()) {
            return binRepo.existsById(bin)
                    .flatMapMany(exists -> exists
                            ? repo.findAll(bin, code, status, page, size)
                            : Flux.error(new NoSuchElementException("BIN no existe")));
        }
        // Sin filtro de BIN: listar normal
        return repo.findAll(null, code, status, page, size);
    }
}

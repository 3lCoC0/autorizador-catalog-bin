package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.ListSubtypesUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import reactor.core.publisher.Flux;

public record ListSubtypesService(SubtypeRepository repo) implements ListSubtypesUseCase {
    @Override
    public Flux<Subtype> execute(String bin, String code, String status, int page, int size) {
        return repo.findAll(bin, code, status, page, size);
    }
}

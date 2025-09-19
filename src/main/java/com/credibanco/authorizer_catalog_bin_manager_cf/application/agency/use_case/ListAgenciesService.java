package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.ListAgenciesUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import reactor.core.publisher.Flux;

import java.util.NoSuchElementException;

public record ListAgenciesService(AgencyRepository repo,
                                  SubtypeReadOnlyRepository subtypeRepo)
        implements ListAgenciesUseCase {

    @Override
    public Flux<Agency> execute(String subtypeCode, String status, String search, int page, int size) {
        // Si no piden filtrar por subtype, listar normal
        if (subtypeCode == null || subtypeCode.isBlank()) {
            return repo.findAll(null, status, search, page, size);
        }
        // Validar existencia del SUBTYPE antes de listar
        return subtypeRepo.existsByCode(subtypeCode)
                .flatMapMany(exists -> exists
                        ? repo.findAll(subtypeCode, status, search, page, size)
                        : Flux.error(new NoSuchElementException("SUBTYPE no encontrado")));
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.ListAgenciesUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import reactor.core.publisher.Flux;

public record ListAgenciesService(AgencyRepository repo) implements ListAgenciesUseCase {
    @Override
    public Flux<Agency> execute(String subtypeCode, String status, String search, int page, int size) {
        return repo.findAll(subtypeCode, status, search, page, size);
    }
}

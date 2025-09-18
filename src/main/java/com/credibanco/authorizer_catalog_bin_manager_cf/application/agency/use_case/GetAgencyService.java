package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.GetAgencyUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record GetAgencyService(AgencyRepository repo) implements GetAgencyUseCase {
    @Override
    public Mono<Agency> execute(String subtypeCode, String agencyCode) {
        return repo.findByPk(subtypeCode, agencyCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("AGENCY no encontrada")));
    }
}

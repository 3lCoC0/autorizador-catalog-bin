package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.GetAgencyUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record GetAgencyService(AgencyRepository repo,
                               SubtypeReadOnlyRepository subtypeRepo)
        implements GetAgencyUseCase {

    @Override
    public Mono<Agency> execute(String subtypeCode, String agencyCode) {
        return subtypeRepo.existsByCode(subtypeCode)
                .flatMap(exists -> exists
                        ? repo.findByPk(subtypeCode, agencyCode)
                        : Mono.error(new NoSuchElementException("SUBTYPE no encontrado"))
                )
                .switchIfEmpty(Mono.error(new NoSuchElementException("AGENCY no encontrada")));
    }
}

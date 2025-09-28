package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.GetAgencyUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

// application/agency/use_case/GetAgencyService.java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record GetAgencyService(AgencyRepository repo,
                               SubtypeReadOnlyRepository subtypeRepo) implements GetAgencyUseCase {
    @Override
    public Mono<Agency> execute(String subtypeCode, String agencyCode) {
        long t0 = System.nanoTime();
        log.debug("UC:Agency:Get:start st={} ag={}", subtypeCode, agencyCode);
        return subtypeRepo.existsByCode(subtypeCode)
                .flatMap(exists -> exists
                        ? repo.findByPk(subtypeCode, agencyCode)
                        : Mono.error(new NoSuchElementException("SUBTYPE no encontrado"))
                )
                .switchIfEmpty(Mono.error(new NoSuchElementException("AGENCY no encontrada")))
                .doOnSuccess(a -> log.info("UC:Agency:Get:done st={} ag={} status={} elapsedMs={}",
                        a.subtypeCode(), a.agencyCode(), a.status(), (System.nanoTime()-t0)/1_000_000));
    }
}

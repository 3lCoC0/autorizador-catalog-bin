package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.ListAgenciesUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import reactor.core.publisher.Flux;

import java.util.NoSuchElementException;

// application/agency/use_case/ListAgenciesService.java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ListAgenciesService(AgencyRepository repo,
                                  SubtypeReadOnlyRepository subtypeRepo) implements ListAgenciesUseCase {

    @Override
    public Flux<Agency> execute(String subtypeCode, String status, String search, int page, int size) {
        long t0 = System.nanoTime();
        log.info("UC:Agency:List:start st={} status={} page={} size={}", subtypeCode, status, page, size);

        Flux<Agency> flux = (subtypeCode == null || subtypeCode.isBlank())
                ? repo.findAll(null, status, search, page, size)
                : subtypeRepo.existsByCode(subtypeCode)
                .flatMapMany(exists -> exists
                        ? repo.findAll(subtypeCode, status, search, page, size)
                        : Flux.error(new NoSuchElementException("SUBTYPE no encontrado")));

        return flux.doOnComplete(() -> log.info("UC:Agency:List:done st={} status={} page={} size={} elapsedMs={}",
                subtypeCode, status, page, size, (System.nanoTime()-t0)/1_000_000));
    }
}

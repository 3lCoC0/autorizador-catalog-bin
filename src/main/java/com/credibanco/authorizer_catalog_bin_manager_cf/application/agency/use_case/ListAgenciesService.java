package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.ListAgenciesUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public record ListAgenciesService(AgencyRepository repo,
                                  SubtypeReadOnlyRepository subtypeRepo) implements ListAgenciesUseCase {

    @Override
    public Flux<Agency> execute(String subtypeCode, String status, String search, int page, int size) {
        long t0 = System.nanoTime();
        log.info("UC:Agency:List:start st={} status={} page={} size={}", subtypeCode, status, page, size);

        if (page < 0 || size <= 0) {
            return Flux.error(new AppException(AppError.AGENCY_INVALID_DATA, "page debe ser >=0 y size > 0"));
        }

        Flux<Agency> flux = (subtypeCode == null || subtypeCode.isBlank())
                ? repo.findAll(null, status, search, page, size)
                : subtypeRepo.existsByCode(subtypeCode)
                .flatMapMany(exists -> exists
                        ? repo.findAll(subtypeCode, status, search, page, size)
                        : Flux.error(new AppException(AppError.SUBTYPE_NOT_FOUND, "subtypeCode=" + subtypeCode)));

        return flux.doOnComplete(() -> log.info("UC:Agency:List:done st={} status={} page={} size={} elapsedMs={}",
                subtypeCode, status, page, size, (System.nanoTime()-t0)/1_000_000));
    }
}

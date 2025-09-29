package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.GetAgencyUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

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
                        : Mono.error(new AppException(AppError.SUBTYPE_NOT_FOUND, "subtypeCode=" + subtypeCode)))
                .switchIfEmpty(Mono.error(new AppException(AppError.AGENCY_NOT_FOUND,
                        "subtypeCode=" + subtypeCode + ", agencyCode=" + agencyCode)))
                .doOnSuccess(a -> log.info("UC:Agency:Get:done st={} ag={} status={} elapsedMs={}",
                        a.subtypeCode(), a.agencyCode(), a.status(), (System.nanoTime()-t0)/1_000_000));
    }
}

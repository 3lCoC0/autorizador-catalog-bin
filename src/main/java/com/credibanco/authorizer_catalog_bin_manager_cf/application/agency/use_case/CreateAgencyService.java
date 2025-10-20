package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.CreateAgencyUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Slf4j
public record CreateAgencyService(AgencyRepository repo,
                                  SubtypeReadOnlyRepository subtypeRepo,
                                  TransactionalOperator tx) implements CreateAgencyUseCase {

    private static long ms(long t0) { return (System.nanoTime()-t0)/1_000_000; }

    @Override
    public Mono<Agency> execute(Agency draft) {
        long t0 = System.nanoTime();
        log.debug("UC:Agency:Create:start st={} ag={}", draft.subtypeCode(), draft.agencyCode());

        return subtypeRepo.existsByCode(draft.subtypeCode())
                .flatMap(exists -> exists
                        ? repo.existsByPk(draft.subtypeCode(), draft.agencyCode())
                        .flatMap(dup -> dup
                                ? Mono.error(new AppException(AppError.AGENCY_ALREADY_EXISTS))
                                : repo.save(draft))
                        : Mono.error(new AppException(AppError.SUBTYPE_NOT_FOUND,
                        "No se encontró el SUBTYPE  " + draft.subtypeCode())))
                .onErrorMap(IllegalArgumentException.class,
                        e -> new AppException(AppError.AGENCY_INVALID_DATA, e.getMessage()))
                .doOnSuccess(a -> log.info("UC:Agency:Create:done st={} ag={} status={} elapsedMs={}",
                        a.subtypeCode(), a.agencyCode(), a.status(), ms(t0)))
                .as(tx::transactional);
    }
}

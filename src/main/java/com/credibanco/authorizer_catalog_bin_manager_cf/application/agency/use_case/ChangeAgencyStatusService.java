package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.ChangeAgencyStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Slf4j
public record ChangeAgencyStatusService(AgencyRepository repo,
                                        SubtypeReadOnlyRepository subtypeRepo,
                                        TransactionalOperator tx) implements ChangeAgencyStatusUseCase {
    private static long ms(long t0) { return (System.nanoTime() - t0) / 1_000_000; }

    @Override
    public Mono<Agency> execute(String subtypeCode, String agencyCode, String newStatus, String by) {
        long t0 = System.nanoTime();
        log.debug("UC:Agency:ChangeStatus:start st={} ag={} newStatus={}", subtypeCode, agencyCode, newStatus);

        if (!"A".equals(newStatus) && !"I".equals(newStatus))
            return Mono.error(new AppException(AppError.AGENCY_INVALID_DATA, "status debe ser 'A' o 'I'"));

        Mono<Void> ensureSubtypeExists = subtypeRepo.existsByCode(subtypeCode)
                .flatMap(exists -> exists ? Mono.empty()
                        : Mono.error(new AppException(AppError.SUBTYPE_NOT_FOUND)));

        return repo.findByPk(subtypeCode, agencyCode)
                .switchIfEmpty(Mono.error(new AppException(AppError.AGENCY_NOT_FOUND)))
                .flatMap(ensureSubtypeExists::thenReturn)
                .flatMap(cur -> {
                    if ("I".equals(newStatus) && "A".equals(cur.status())) {
                        // Política: no dejar al SUBTYPE sin ninguna agencia activa
                        return repo.existsAnotherActive(subtypeCode, agencyCode)
                                .flatMap(existsOther -> existsOther
                                        ? Mono.just(cur)
                                        : Mono.error(new AppException(AppError.AGENCY_CONFLICT_RULE,
                                        "No puede inactivarse la única AGENCY activa del SUBTYPE " + subtypeCode)));
                    }
                    return Mono.just(cur);
                })
                .map(cur -> cur.changeStatus(newStatus, by))
                .onErrorMap(IllegalArgumentException.class,
                        e -> new AppException(AppError.AGENCY_INVALID_DATA, e.getMessage()))
                .flatMap(repo::save)
                .doOnSuccess(a -> log.info("UC:Agency:ChangeStatus:done st={} ag={} status={} elapsedMs={}",
                        a.subtypeCode(), a.agencyCode(), a.status(), ms(t0)))
                .as(tx::transactional);
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.ChangeAgencyStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ChangeAgencyStatusService(AgencyRepository repo,
                                        SubtypeReadOnlyRepository subtypeRepo,
                                        TransactionalOperator tx) implements ChangeAgencyStatusUseCase {
    private static long ms(long t0) {
        return (System.nanoTime() - t0) / 1_000_000;
    }

    @Override
    public Mono<Agency> execute(String subtypeCode, String agencyCode, String newStatus, String by) {
        long t0 = System.nanoTime();
        log.debug("UC:Agency:ChangeStatus:start st={} ag={} newStatus={}", subtypeCode, agencyCode, newStatus);

        if (!"A".equals(newStatus) && !"I".equals(newStatus))
            return Mono.error(new IllegalArgumentException("status inválido"));

        Mono<Void> ensureSubtypeExists = subtypeRepo.existsByCode(subtypeCode)
                .flatMap(exists -> exists ? Mono.empty()
                        : Mono.error(new IllegalArgumentException("SUBTYPE no existe")));

        return repo.findByPk(subtypeCode, agencyCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("AGENCY no encontrada")))
                .flatMap(ensureSubtypeExists::thenReturn)
                .flatMap(cur -> {
                    if ("I".equals(newStatus) && "A".equals(cur.status())) {

                        return repo.existsAnotherActive(subtypeCode, agencyCode)
                                .flatMap(existsOther -> {
                                    if (!existsOther) {
                                        return Mono.error(new IllegalStateException(
                                                "No puede inactivarse la única AGENCY activa del SUBTYPE (" + subtypeCode + "). " +
                                                        "Cree otra AGENCY activa o inhabilite el SUBTYPE."
                                        ));
                                    }
                                    return Mono.just(cur);
                                });
                    }
                    return Mono.just(cur);
                })
                .map(cur -> cur.changeStatus(newStatus, by))
                .flatMap(repo::save)
                .doOnSuccess(a -> log.info("UC:Agency:ChangeStatus:done st={} ag={} status={} elapsedMs={}",
                        a.subtypeCode(), a.agencyCode(), a.status(), (System.nanoTime() - t0) / 1_000_000))
                .as(tx::transactional);
    }
}
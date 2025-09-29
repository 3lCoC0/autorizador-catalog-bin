package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.UpdateAgencyUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Slf4j
public record UpdateAgencyService(AgencyRepository repo,
                                  SubtypeReadOnlyRepository subtypeRepo,
                                  TransactionalOperator tx) implements UpdateAgencyUseCase {

    private static long ms(long t0) { return (System.nanoTime()-t0)/1_000_000; }

    @Override
    public Mono<Agency> execute(Agency updated) {
        long t0 = System.nanoTime();
        log.debug("UC:Agency:Update:start st={} ag={}", updated.subtypeCode(), updated.agencyCode());

        return subtypeRepo.existsByCode(updated.subtypeCode())
                .flatMap(exists -> exists
                        ? repo.findByPk(updated.subtypeCode(), updated.agencyCode())
                        : Mono.error(new AppException(AppError.SUBTYPE_NOT_FOUND, "subtypeCode=" + updated.subtypeCode()))
                )
                .switchIfEmpty(Mono.error(new AppException(AppError.AGENCY_NOT_FOUND,
                        "subtypeCode=" + updated.subtypeCode() + ", agencyCode=" + updated.agencyCode())))
                .flatMap(current -> {
                    Agency merged;
                    try {
                        merged = current.updateBasics(
                                updated.name(),
                                updated.agencyNit(),
                                updated.address(),
                                updated.phone(),
                                updated.municipalityDaneCode(),
                                updated.embosserHighlight(),
                                updated.embosserPins(),
                                updated.cardCustodianPrimary(),
                                updated.cardCustodianPrimaryId(),
                                updated.cardCustodianSecondary(),
                                updated.cardCustodianSecondaryId(),
                                updated.pinCustodianPrimary(),
                                updated.pinCustodianPrimaryId(),
                                updated.pinCustodianSecondary(),
                                updated.pinCustodianSecondaryId(),
                                updated.description(),
                                updated.updatedBy()
                        );
                    } catch (IllegalArgumentException iae) {
                        return Mono.error(new AppException(AppError.AGENCY_INVALID_DATA, iae.getMessage()));
                    }
                    return repo.save(merged);
                })
                .doOnSuccess(a -> log.info("UC:Agency:Update:done st={} ag={} elapsedMs={}",
                        a.subtypeCode(), a.agencyCode(), ms(t0)))
                .as(tx::transactional);
    }
}

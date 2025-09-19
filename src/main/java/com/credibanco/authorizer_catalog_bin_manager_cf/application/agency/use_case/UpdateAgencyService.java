package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.UpdateAgencyUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record UpdateAgencyService(
        AgencyRepository repo,
        SubtypeReadOnlyRepository subtypeRepo,
        TransactionalOperator tx
) implements UpdateAgencyUseCase {

    @Override
    public Mono<Agency> execute(Agency updated) {
        // 1) Validar existencia de SUBTYPE
        return subtypeRepo.existsByCode(updated.subtypeCode())
                .flatMap(exists -> exists
                        ? repo.findByPk(updated.subtypeCode(), updated.agencyCode())
                        : Mono.error(new NoSuchElementException("SUBTYPE no encontrado"))
                )
                // 2) Validar existencia de AGENCY
                .switchIfEmpty(Mono.error(new NoSuchElementException("AGENCY no encontrada")))
                // 3) Fusionar manteniendo el status actual y timestamps por dominio
                .flatMap(current -> {
                    Agency merged = current.updateBasics(
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
                    return repo.save(merged);
                })
                .as(tx::transactional);
    }
}

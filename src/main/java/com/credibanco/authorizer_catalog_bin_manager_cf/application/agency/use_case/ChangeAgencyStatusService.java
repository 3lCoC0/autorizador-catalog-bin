package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.ChangeAgencyStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record ChangeAgencyStatusService(
        AgencyRepository repo, SubtypeReadOnlyRepository subtypeRepo, TransactionalOperator tx
) implements ChangeAgencyStatusUseCase {

    @Override
    public Mono<Agency> execute(String subtypeCode, String agencyCode, String newStatus, String by) {
        if (!"A".equals(newStatus) && !"I".equals(newStatus))
            return Mono.error(new IllegalArgumentException("status inválido"));

        Mono<Void> ensureSubtypeActive = "A".equals(newStatus)
                ? subtypeRepo.isActive(subtypeCode).flatMap(a -> a ? Mono.empty()
                : Mono.error(new IllegalStateException("No se puede activar: SUBTYPE inactivo")))
                : Mono.empty();

        return repo.findByPk(subtypeCode, agencyCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("AGENCY no encontrada")))
                .flatMap(current -> ensureSubtypeActive.thenReturn(current))
                .map(cur -> cur.changeStatus(newStatus, by))
                .flatMap(repo::save)
                .as(tx::transactional);
    }
}

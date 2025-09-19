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

        // Solo aseguramos que el SUBTYPE exista (independiente del status)
        Mono<Void> ensureSubtypeExists = subtypeRepo.existsByCode(subtypeCode)
                .flatMap(exists -> exists ? Mono.empty()
                        : Mono.error(new IllegalArgumentException("SUBTYPE no existe")));

        return repo.findByPk(subtypeCode, agencyCode)
                .switchIfEmpty(Mono.error(new java.util.NoSuchElementException("AGENCY no encontrada")))
                .flatMap(cur -> ensureSubtypeExists.thenReturn(cur))
                .map(cur -> cur.changeStatus(newStatus, by))
                .flatMap(repo::save)
                .as(tx::transactional);
    }
}


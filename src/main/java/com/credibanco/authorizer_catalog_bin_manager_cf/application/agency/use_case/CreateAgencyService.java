package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.CreateAgencyUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public record CreateAgencyService(
        AgencyRepository repo,
        SubtypeReadOnlyRepository subtypeRepo,
        TransactionalOperator tx
) implements CreateAgencyUseCase {

    @Override
    public Mono<Agency> execute(Agency draft) {
        return subtypeRepo.isActive(draft.subtypeCode())
                .flatMap(active -> active
                        ? repo.existsByPk(draft.subtypeCode(), draft.agencyCode())
                        .flatMap(exists -> exists
                                ? Mono.error(new IllegalStateException("Agency ya existe"))
                                : repo.save(draft))
                        : Mono.error(new IllegalStateException("No se puede crear: SUBTYPE inactivo o inexistente"))
                )
                .as(tx::transactional);
    }
}

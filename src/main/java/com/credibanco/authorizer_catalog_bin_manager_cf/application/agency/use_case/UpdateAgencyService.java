package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.UpdateAgencyUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record UpdateAgencyService(AgencyRepository repo, TransactionalOperator tx)
        implements UpdateAgencyUseCase {
    @Override
    public Mono<Agency> execute(Agency updated) {
        return repo.findByPk(updated.subtypeCode(), updated.agencyCode())
                .switchIfEmpty(Mono.error(new NoSuchElementException("AGENCY no encontrada")))
                .flatMap(current -> repo.save(updated))
                .as(tx::transactional);
    }
}

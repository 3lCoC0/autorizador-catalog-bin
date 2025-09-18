package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.ChangeSubtypeStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.AgencyReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record ChangeSubtypeStatusService(
        SubtypeRepository repo,
        AgencyReadOnlyRepository agencyRepo,
        TransactionalOperator tx
) implements ChangeSubtypeStatusUseCase {

    @Override
    public Mono<Subtype> execute(String bin, String subtypeCode, String newStatus, String by) {
        if (!"A".equals(newStatus) && !"I".equals(newStatus)) {
            return Mono.error(new IllegalArgumentException("status inválido"));
        }

        return repo.findByPk(bin, subtypeCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("SUBTYPE no encontrado")))
                .flatMap(s -> {
                    if ("A".equals(newStatus)) {
                        return agencyRepo.countActiveBySubtypeCode(s.subtypeCode())
                                .flatMap(cnt -> (cnt > 0)
                                        ? repo.save(s.changeStatus("A", by))
                                        : Mono.error(new IllegalStateException(
                                        "El SUBTYPE activo debe tener al menos una AGENCY activa.")));
                    }
                    return repo.save(s.changeStatus("I", by));
                })
                .as(tx::transactional);
    }
}

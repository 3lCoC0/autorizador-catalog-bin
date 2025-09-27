package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.ChangeSubtypeStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.AgencyReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ChangeSubtypeStatusService(
        SubtypeRepository repo,
        AgencyReadOnlyRepository agencyRepo,
        TransactionalOperator tx
) implements ChangeSubtypeStatusUseCase {

    private static long ms(long t0) { return (System.nanoTime() - t0) / 1_000_000; }

    @Override
    public Mono<Subtype> execute(String bin, String subtypeCode, String newStatus, String by) {
        long t0 = System.nanoTime();
        log.debug("UC:Subtype:ChangeStatus:start bin={} code={} newStatus={}", bin, subtypeCode, newStatus);

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
                .doOnSuccess(s -> log.info("UC:Subtype:ChangeStatus:done bin={} code={} status={} elapsedMs={}",
                        s.bin(), s.subtypeCode(), s.status(), ms(t0)))
                .as(tx::transactional);
    }
}


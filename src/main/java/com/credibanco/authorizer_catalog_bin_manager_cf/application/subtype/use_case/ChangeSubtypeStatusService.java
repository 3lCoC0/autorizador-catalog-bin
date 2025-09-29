package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.ChangeSubtypeStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.AgencyReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

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
            return Mono.error(new AppException(AppError.SUBTYPE_INVALID_DATA, "status debe ser 'A' o 'I'"));
        }

        return repo.findByPk(bin, subtypeCode)
                .switchIfEmpty(Mono.error(new AppException(AppError.SUBTYPE_NOT_FOUND, "bin=" + bin + ", code=" + subtypeCode)))
                .flatMap(s -> {
                    if ("A".equals(newStatus)) {
                        return agencyRepo.countActiveBySubtypeCode(s.subtypeCode())
                                .flatMap(cnt -> (cnt > 0)
                                        ? Mono.just(s)
                                        : Mono.error(new AppException(AppError.SUBTYPE_ACTIVATE_REQUIRES_AGENCY,
                                        "Para activar SUBTYPE se requiere al menos una AGENCY activa")))
                                .map(ok -> ok.changeStatus("A", by));
                    }
                    return Mono.just(s.changeStatus("I", by));
                })
                .onErrorMap(IllegalArgumentException.class,
                        e -> new AppException(AppError.SUBTYPE_INVALID_DATA, e.getMessage()))
                .flatMap(repo::save)
                .doOnSuccess(s -> log.info("UC:Subtype:ChangeStatus:done bin={} code={} status={} elapsedMs={}",
                        s.bin(), s.subtypeCode(), s.status(), ms(t0)))
                .as(tx::transactional);
    }
}

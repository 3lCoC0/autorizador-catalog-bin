package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.GetSubtypeUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public record GetSubtypeService(SubtypeRepository repo) implements GetSubtypeUseCase {
    @Override
    public Mono<Subtype> execute(String bin, String subtypeCode) {
        long t0 = System.nanoTime();
        return repo.findByPk(bin, subtypeCode)
                .switchIfEmpty(Mono.error(new AppException(AppError.SUBTYPE_NOT_FOUND,
                        "bin=" + bin + ", code=" + subtypeCode)))
                .doOnSuccess(s -> {
                    long elapsed = (System.nanoTime() - t0) / 1_000_000;
                    log.info("UC:Subtype:Get:done bin={} code={} status={} elapsedMs={}",
                            s.bin(), s.subtypeCode(), s.status(), elapsed);
                });
    }
}

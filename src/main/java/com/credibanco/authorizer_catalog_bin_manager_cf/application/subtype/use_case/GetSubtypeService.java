package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.GetSubtypeUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record GetSubtypeService(SubtypeRepository repo) implements GetSubtypeUseCase {
    @Override
    public Mono<Subtype> execute(String bin, String subtypeCode) {
        long t0 = System.nanoTime();
        log.debug("UC:Subtype:Get:start bin={} code={}", bin, subtypeCode);
        return repo.findByPk(bin, subtypeCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("SUBTYPE no encontrado")))
                .doOnSuccess(s -> log.info("UC:Subtype:Get:done bin={} code={} status={} elapsedMs={}",
                        s.bin(), s.subtypeCode(), s.status(), (System.nanoTime()-t0)/1_000_000));
    }
}

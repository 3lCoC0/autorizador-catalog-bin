package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.ListSubtypesUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import reactor.core.publisher.Flux;

import java.util.NoSuchElementException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ListSubtypesService(
        SubtypeRepository repo,
        BinReadOnlyRepository binRepo
) implements ListSubtypesUseCase {

    @Override
    public Flux<Subtype> execute(String bin, String code, String status, int page, int size) {
        long t0 = System.nanoTime();
        log.info("UC:Subtype:List:start bin={} code={} status={} page={} size={}", bin, code, status, page, size);

        Flux<Subtype> flux = (bin != null && !bin.isBlank())
                ? binRepo.existsById(bin)
                .flatMapMany(exists -> exists ? repo.findAll(bin, code, status, page, size)
                        : Flux.error(new NoSuchElementException("BIN no existe")))
                : repo.findAll(null, code, status, page, size);

        return flux.doOnComplete(() -> log.info("UC:Subtype:List:done bin={} code={} status={} page={} size={} elapsedMs={}",
                bin, code, status, page, size, (System.nanoTime()-t0)/1_000_000));
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.ListBinsUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public class ListBinsService implements ListBinsUseCase {
    private final BinRepository repo;
    public ListBinsService(BinRepository repo) { this.repo = repo; }

    private static long ms(long t0) { return (System.nanoTime() - t0) / 1_000_000; }

    @Override
    public Flux<Bin> execute(int page, int size) {
        long t0 = System.nanoTime();
        log.info("UC:ListBins:start page={}, size={}", page, size);

        return repo.findAll(page, size)
                .doOnComplete(() -> log.info("UC:ListBins:done page={}, size={}, elapsedMs={}",
                        page, size, ms(t0)));
    }
}

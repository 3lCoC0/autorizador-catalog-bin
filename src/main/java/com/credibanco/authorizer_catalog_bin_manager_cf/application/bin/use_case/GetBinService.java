package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.GetBinUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public record GetBinService(BinRepository repo) implements GetBinUseCase {

    private static long ms(long t0) {
        return (System.nanoTime() - t0) / 1_000_000;
    }

    @Override
    public Mono<Bin> execute(String bin) {
        long t0 = System.nanoTime();
        log.debug("UC:GetBin:start bin={}", bin);

        return repo.findById(bin)
                .switchIfEmpty(Mono.error(new AppException(AppError.BIN_NOT_FOUND)))
                .doOnSuccess(b -> log.info("UC:GetBin:done bin={}, status={}, elapsedMs={}",
                        b.bin(), b.status(), ms(t0)));
    }
}

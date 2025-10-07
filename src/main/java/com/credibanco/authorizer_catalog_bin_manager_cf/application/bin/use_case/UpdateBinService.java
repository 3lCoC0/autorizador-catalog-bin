package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.UpdateBinUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.SubtypeReadOnlyRepository;

import java.util.Objects;

@Slf4j
public record UpdateBinService(BinRepository repo, SubtypeReadOnlyRepository subtypeRepo, TransactionalOperator tx)
        implements UpdateBinUseCase {

    private static long ms(long t0) { return (System.nanoTime() - t0) / 1_000_000; }

    @Override
    public Mono<Bin> execute(String bin, String name, String typeBin, String typeAccount,
                             String compensationCod, String description,
                             String usesBinExt, Integer binExtDigits,
                             String updatedByNullable) {

        long t0 = System.nanoTime();
        log.debug("UC:UpdateBin:start bin={}, usesExt={}, extDigits={}", bin, usesBinExt, binExtDigits);

        return repo.findById(bin)
                .switchIfEmpty(Mono.error(new AppException(AppError.BIN_NOT_FOUND)))
                .flatMap(current -> validateSubtypeRestriction(current, usesBinExt, binExtDigits)
                        .then(Mono.defer(() -> Mono.just(
                                        current.updateBasics(name, typeBin, typeAccount,
                                                compensationCod, description,
                                                usesBinExt, binExtDigits, updatedByNullable)))
                                .onErrorMap(IllegalArgumentException.class,
                                        e -> new AppException(AppError.BIN_INVALID_DATA, e.getMessage()))
                                .flatMap(repo::save))
                )
                .doOnSuccess(b -> log.info("UC:UpdateBin:done bin={}, elapsedMs={}", b.bin(), ms(t0)))
                .as(tx::transactional);
    }

    private Mono<Void> validateSubtypeRestriction(Bin current, String newUsesBinExt, Integer newBinExtDigits) {
        boolean wantsYWithOneDigit = "Y".equals(newUsesBinExt) && Integer.valueOf(1).equals(newBinExtDigits);
        boolean changedConfig = !Objects.equals(current.usesBinExt(), newUsesBinExt)
                || !Objects.equals(current.binExtDigits(), newBinExtDigits);

        if (!wantsYWithOneDigit || !changedConfig) {
            return Mono.empty();
        }

        return subtypeRepo.existsAnyByBin(current.bin())
                .flatMap(exists -> exists
                        ? Mono.error(new AppException(AppError.BIN_INVALID_DATA,
                        "no se pueden cambiar campos indicados debido a ya existencia de subtypes asignados"))
                        : Mono.empty());
    }
}

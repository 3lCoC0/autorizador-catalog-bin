package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.CreateBinUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Slf4j
public record CreateBinService(BinRepository repo, TransactionalOperator tx)
        implements CreateBinUseCase {

    @Override
    public Mono<Bin> execute(String bin, String name, String typeBin, String typeAccount,
                             String compensationCod, String description,
                             String usesBinExt, Integer binExtDigits,
                             String createdByNullable) {
        long t0 = System.nanoTime();
        log.debug("UC:CreateBin:start bin={}, usesExt={}, extDigits={}", bin, usesBinExt, binExtDigits);

        return Mono.defer(() -> {
                    // Defensa mínima en UC (el dominio también valida)
                    if (bin == null || bin.length() < 6 || bin.length() > 9) {
                        return Mono.error(new AppException(AppError.BIN_INVALID_DATA,
                                "BIN inválido (debe tener 6 a 9 dígitos)"));
                    }

                    return Mono.fromCallable(() ->
                                    Bin.createNew(bin, name, typeBin, typeAccount,
                                            compensationCod, description,
                                            usesBinExt, binExtDigits, createdByNullable))
                            .onErrorMap(IllegalArgumentException.class,
                                    e -> new AppException(AppError.BIN_INVALID_DATA, e.getMessage()));
                })
                .flatMap(aggregate -> repo.existsById(bin)
                        .flatMap(exists -> exists
                                ? Mono.error(new AppException(AppError.BIN_ALREADY_EXISTS))
                                : repo.save(aggregate)))
                .doOnSuccess(b -> log.info("UC:CreateBin:done bin={}, elapsedMs={}",
                        b.bin(), (System.nanoTime() - t0) / 1_000_000))
                .as(tx::transactional);
    }
}

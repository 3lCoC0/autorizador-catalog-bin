package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.CreateBinUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record CreateBinService(BinRepository repo, TransactionalOperator tx)
        implements CreateBinUseCase {

    @Override
    public Mono<Bin> execute(String bin, String name, String typeBin, String typeAccount,
                             String compensationCod, String description,
                             String usesBinExt, Integer binExtDigits,
                             String createdByNullable) {
        long t0 = System.nanoTime();
        return Mono.defer(() -> {
            log.debug("UC:CreateBin:start bin={}, usesExt={}, extDigits={}", bin, usesBinExt, binExtDigits);
            if (bin == null || bin.length() < 6 || bin.length() > 9) {
                return Mono.error(new IllegalArgumentException("BIN inválido (debe tener 6 a 9 dígitos)"));
            }
            Bin aggregate = Bin.createNew(bin, name, typeBin, typeAccount, compensationCod, description,
                    usesBinExt, binExtDigits, createdByNullable);

            return repo.existsById(bin)
                    .flatMap(exists -> exists
                            ? Mono.error(new IllegalStateException("El BIN ya existe"))
                            : repo.save(aggregate))
                    .doOnSuccess(b -> log.info("UC:CreateBin:done bin={}, elapsedMs={}", b.bin(), (System.nanoTime()-t0)/1_000_000))
                    .as(tx::transactional);
        });
    }
}
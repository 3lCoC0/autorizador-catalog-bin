package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.ChangeBinStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record ChangeBinStatusService(BinRepository repo, TransactionalOperator tx)
        implements ChangeBinStatusUseCase {
    @Override
    public Mono<Bin> execute(String bin, String newStatus, String byNullable) {
        return repo.findById(bin)
                .switchIfEmpty(Mono.error(new NoSuchElementException("BIN no existe")))
                .flatMap(current -> repo.save(current.changeStatus(newStatus, byNullable)))
                .as(tx::transactional);
    }
}
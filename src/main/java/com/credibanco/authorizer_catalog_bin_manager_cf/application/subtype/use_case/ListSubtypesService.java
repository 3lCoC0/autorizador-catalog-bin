package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.ListSubtypesUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public record ListSubtypesService(
        SubtypeRepository repo,
        BinReadOnlyRepository binRepo
) implements ListSubtypesUseCase {

    @Override
    public Flux<Subtype> execute(String bin, String code, String status, int page, int size) {
        long t0 = System.nanoTime();
        log.info("UC:Subtype:List:start bin={} code={} status={} page={} size={}", bin, code, status, page, size);

        if (page < 0 || size <= 0) {
            return Flux.error(new AppException(AppError.SUBTYPE_INVALID_DATA, "page debe ser >=0 y size > 0"));
        }

        Flux<Subtype> flux = (bin != null && !bin.isBlank())
                ? binRepo.existsById(bin).flatMapMany(exists -> exists
                ? repo.findAll(bin, code, status, page, size)
                : Flux.error(new AppException(AppError.BIN_NOT_FOUND, "bin=" + bin)))
                : repo.findAll(null, code, status, page, size);

        return flux.doOnComplete(() -> {
            long elapsed = (System.nanoTime() - t0) / 1_000_000;
            log.info("UC:Subtype:List:done bin={} code={} status={} page={} size={} elapsedMs={}",
                    bin, code, status, page, size, elapsed);
        });
    }
}

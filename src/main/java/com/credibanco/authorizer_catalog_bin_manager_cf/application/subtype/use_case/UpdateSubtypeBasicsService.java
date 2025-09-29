package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.UpdateSubtypeBasicsUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Slf4j
public record UpdateSubtypeBasicsService(
        SubtypeRepository repo,
        BinReadOnlyRepository binRepo,
        TransactionalOperator tx
) implements UpdateSubtypeBasicsUseCase {

    private static long ms(long t0) { return (System.nanoTime() - t0) / 1_000_000; }

    @Override
    public Mono<Subtype> execute(String bin, String subtypeCode,
                                 String name, String description,
                                 String ownerIdType, String ownerIdNumber,
                                 String newBinExt, String updatedByNullable) {

        long t0 = System.nanoTime();
        log.debug("UC:Subtype:Update:start bin={} code={} ownerIdType={} ext?={}",
                bin, subtypeCode, ownerIdType, newBinExt != null);

        return repo.findByPk(bin, subtypeCode)
                .switchIfEmpty(Mono.error(new AppException(AppError.SUBTYPE_NOT_FOUND)))
                .flatMap(current ->
                        binRepo.getExtConfig(current.bin())
                                .switchIfEmpty(Mono.error(new AppException(AppError.BIN_NOT_FOUND)))
                                .flatMap(cfg -> {
                                    String normExt;
                                    try {
                                        normExt = normalizeExtAgainstConfig(current.bin(), newBinExt, cfg.usesBinExt(), cfg.binExtDigits());
                                    } catch (IllegalArgumentException iae) {
                                        return Mono.error(new AppException(AppError.SUBTYPE_INVALID_DATA, iae.getMessage()));
                                    }

                                    Subtype updated;
                                    try {
                                        updated = current.updateBasics(name, description, ownerIdType, ownerIdNumber, normExt, updatedByNullable);
                                    } catch (IllegalArgumentException iae) {
                                        return Mono.error(new AppException(AppError.SUBTYPE_INVALID_DATA, iae.getMessage()));
                                    }

                                    boolean extChanged = (current.binExt() == null && updated.binExt() != null)
                                            || (current.binExt() != null && !current.binExt().equals(updated.binExt()));

                                    Mono<Boolean> extExists = extChanged && updated.binExt() != null
                                            ? repo.existsByBinAndExt(updated.bin(), updated.binExt())
                                            : Mono.just(false);

                                    return extExists.flatMap(exists -> exists
                                            ? Mono.error(new AppException(AppError.SUBTYPE_ALREADY_EXISTS, "bin_ext ya usado para ese BIN"))
                                            : repo.save(updated));
                                })
                )
                .doOnSuccess(s -> log.info("UC:Subtype:Update:done bin={} code={} elapsedMs={}",
                        s.bin(), s.subtypeCode(), ms(t0)))
                .as(tx::transactional);
    }

    private static String normalizeExtAgainstConfig(String bin, String rawExt, String uses, Integer digits) {
        int baseLen = bin == null ? 0 : bin.length();
        int maxLen = 9 - baseLen;

        if ("Y".equalsIgnoreCase(uses)) {
            if (baseLen == 9) throw new IllegalArgumentException("Config inválida: BIN(9) no admite extensión");
            if (digits == null || digits < 1 || digits > maxLen)
                throw new IllegalArgumentException("Config inválida: BIN_EXT_DIGITS fuera de rango (máx " + maxLen + ")");
            if (rawExt == null || rawExt.isBlank())
                throw new IllegalArgumentException("bin_ext es requerido por configuración del BIN");

            String d = rawExt.replaceAll("\\D", "");
            if (d.length() > digits)
                throw new IllegalArgumentException("bin_ext no puede exceder " + digits + " dígitos");
            d = String.format("%0" + digits + "d", Integer.parseInt(d));

            if (baseLen + d.length() > 9)
                throw new IllegalArgumentException("BIN + extensión excede 9 dígitos");

            return d;
        } else {
            return null;
        }
    }
}

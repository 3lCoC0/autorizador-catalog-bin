// application/subtype/use_case/UpdateSubtypeBasicsService.java
package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.UpdateSubtypeBasicsUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record UpdateSubtypeBasicsService(
        SubtypeRepository repo,
        BinReadOnlyRepository binRepo,
        TransactionalOperator tx
) implements UpdateSubtypeBasicsUseCase {

    @Override
    public Mono<Subtype> execute(String bin, String subtypeCode,
                                 String name, String description,
                                 String ownerIdType, String ownerIdNumber,
                                 String newBinExt, String updatedByNullable) {

        return repo.findByPk(bin, subtypeCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("SUBTYPE no encontrado")))
                .flatMap(current -> binRepo.getExtConfig(current.bin())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("BIN no existe (FK)")))
                        .flatMap(cfg -> {
                            String normExt = normalizeExtAgainstConfig(current.bin(), newBinExt,
                                    cfg.usesBinExt(), cfg.binExtDigits());
                            Subtype updated = current.updateBasics(
                                    name, description, ownerIdType, ownerIdNumber, normExt, updatedByNullable);

                            boolean extChanged = (current.binExt() == null && updated.binExt() != null)
                                    || (current.binExt() != null && !current.binExt().equals(updated.binExt()));

                            Mono<Boolean> extExists = extChanged && updated.binExt() != null
                                    ? repo.existsByBinAndExt(updated.bin(), updated.binExt())
                                    : Mono.just(false);

                            Mono<Boolean> collision = extChanged && updated.binExt() != null
                                    ? binRepo.existsById(updated.binEfectivo())
                                    : Mono.just(false);

                            return Mono.zip(extExists, collision)
                                    .flatMap(z -> {
                                        if (z.getT1()) return Mono.error(new IllegalStateException("bin_ext ya usado para ese BIN"));
                                        if (z.getT2()) return Mono.error(new IllegalStateException(
                                                "Colisión: ya existe BIN maestro " + updated.binEfectivo()));
                                        return repo.save(updated);
                                    });
                        }))
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

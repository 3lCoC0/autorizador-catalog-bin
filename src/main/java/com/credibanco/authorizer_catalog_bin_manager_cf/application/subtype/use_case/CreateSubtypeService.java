package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.CreateSubtypeUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.IdTypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import java.util.List;

@Slf4j
public record CreateSubtypeService(
        SubtypeRepository repo,
        BinReadOnlyRepository binRepo,
        IdTypeReadOnlyRepository idTypeRepo,
        TransactionalOperator tx
) implements CreateSubtypeUseCase {

    private static long ms(long t0) { return (System.nanoTime() - t0) / 1_000_000; }

    @Override
    public Mono<Subtype> execute(String subtypeCode, String bin, String name, String description,
                                 String ownerIdType, String ownerIdNumber, String binExt, String createdBy) {
        long t0 = System.nanoTime();
        log.debug("UC:Subtype:Create:start bin={} code={} ownerIdType={} usesBy={}", bin, subtypeCode, ownerIdType, createdBy);

        Mono<BinReadOnlyRepository.BinExtConfig> cfgMono = binRepo.getExtConfig(bin)
                .switchIfEmpty(Mono.error(new AppException(AppError.BIN_NOT_FOUND)));

        Mono<Boolean> fkIdTypeOk = (ownerIdType == null || ownerIdType.isBlank())
                ? Mono.just(true) : idTypeRepo.existsById(ownerIdType);

        Mono<List<String>> availableIdTypes = (ownerIdType == null || ownerIdType.isBlank())
                ? Mono.just(List.of()) : idTypeRepo.findAllCodes();

        return Mono.zip(cfgMono, fkIdTypeOk, availableIdTypes)
                .flatMap(t -> {
                    var cfg = t.getT1();
                    boolean idTypeOk = t.getT2();
                    List<String> idTypes = t.getT3();
                    if (!idTypeOk)
                        return Mono.error(new AppException(AppError.SUBTYPE_INVALID_DATA,
                                ownerIdTypeError(ownerIdType, idTypes)));

                    log.debug("UC:Subtype:Create:binConfig bin={} uses={} digits={}", bin, cfg.usesBinExt(), cfg.binExtDigits());

                    String normExt;
                    try {
                        normExt = normalizeExtAgainstConfig(bin, binExt, cfg.usesBinExt(), cfg.binExtDigits());
                    } catch (IllegalArgumentException iae) {
                        return Mono.error(new AppException(AppError.SUBTYPE_INVALID_DATA, iae.getMessage()));
                    }

                    Subtype draft;
                    try {
                        draft = Subtype.createNew(subtypeCode, bin, name, description,
                                        ownerIdType, ownerIdNumber, normExt, createdBy)
                                .changeStatus("I", createdBy);
                    } catch (IllegalArgumentException iae) {
                        return Mono.error(new AppException(AppError.SUBTYPE_INVALID_DATA, iae.getMessage()));
                    }

                    Mono<Boolean> bin9Collision = (normExt != null)
                            ? binRepo.existsById(draft.binEfectivo()) : Mono.just(false);
                    Mono<Boolean> pkExists = repo.existsByPk(draft.bin(), draft.subtypeCode());
                    Mono<Boolean> extExists = (normExt != null) ? repo.existsByBinAndExt(draft.bin(), normExt) : Mono.just(false);
                    Mono<Boolean> codeExists = repo.existsBySubtypeCode(draft.subtypeCode());

                    return Mono.zip(bin9Collision, pkExists, extExists, codeExists)
                            .flatMap(z -> {
                                if (z.getT1()) return Mono.error(new AppException(AppError.BIN_ALREADY_EXISTS,
                                        "Colisión: ya existe BIN maestro " + draft.binEfectivo()));
                                if (z.getT2()) return Mono.error(new AppException(AppError.SUBTYPE_ALREADY_EXISTS,
                                        "Ya existe SUBTYPE para ese BIN y código"));
                                if (z.getT3()) return Mono.error(new AppException(AppError.SUBTYPE_ALREADY_EXISTS,
                                        "bin_ext ya usado para ese BIN"));
                                if (z.getT4()) return Mono.error(new AppException(AppError.SUBTYPE_ALREADY_EXISTS,
                                        "Ya existe SUBTYPE con ese código"));
                                return repo.save(draft);
                            });
                })
                .doOnSuccess(s -> log.info("UC:Subtype:Create:done bin={} code={} status={} elapsedMs={}",
                        s.bin(), s.subtypeCode(), s.status(), ms(t0)))
                .as(tx::transactional);
    }

    private static String ownerIdTypeError(String ownerIdType, List<String> available) {
        if (available == null || available.isEmpty()) {
            return "OWNER_ID_TYPE no existe: " + ownerIdType + ". No hay tipos de identificación configurados.";
        }
        return "OWNER_ID_TYPE  "  + ownerIdType +  " no existe. Valores permitidos: " + String.join(", ", available);
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
            if (d.length() > digits) throw new IllegalArgumentException("bin_ext no puede exceder " + digits + " dígitos");
            d = String.format("%0" + digits + "d", Integer.parseInt(d));
            if (baseLen + d.length() > 9) throw new IllegalArgumentException("BIN + extensión excede 9 dígitos");
            return d;
        } else {
            if (rawExt != null && !rawExt.isBlank()) {
                throw new IllegalArgumentException("bin_ext no permitido para BIN sin extensión");
            }
            return null;
        }
    }
}

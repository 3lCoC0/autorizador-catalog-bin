package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.MapRuleUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Slf4j
public record MapRuleService(
        ValidationRepository validations,
        ValidationMapRepository maps,
        SubtypeReadOnlyRepository subtypes,
        TransactionalOperator tx
) implements MapRuleUseCase {

    @Override
    public Mono<ValidationMap> attach(String subtypeCode, String bin, String validationCode, Object value, String by) {
        if (value == null) {
            return Mono.<ValidationMap>error(new AppException(AppError.RULES_MAP_INVALID_DATA, "value es requerido"));
        }

        Mono<Void> ensureSubtype = subtypes.existsByCode(subtypeCode)
                .flatMap(ok -> ok
                        ? Mono.empty()
                        : Mono.<Void>error(new AppException(AppError.SUBTYPE_NOT_FOUND, "subtypeCode=" + subtypeCode)));

        Mono<Void> ensurePair = subtypes.existsByCodeAndBinEfectivo(subtypeCode, bin)
                .flatMap(ok -> ok
                        ? Mono.empty()
                        : Mono.<Void>error(new AppException(AppError.BIN_NOT_FOUND,
                        "BIN efectivo " + bin + " no existe para SUBTYPE " + subtypeCode)));

        var now = OffsetDateTime.now();

        return ensureSubtype.then(ensurePair)
                .then(validations.findByCode(validationCode)
                        .switchIfEmpty(Mono.<com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation>error(
                                new AppException(AppError.RULES_VALIDATION_NOT_FOUND, "code=" + validationCode))))
                .flatMap(v -> {
                    boolean vigente = "A".equals(v.status())
                            && (v.validFrom() == null || !v.validFrom().isAfter(now))
                            && (v.validTo() == null || !v.validTo().isBefore(now));

                    if (!vigente) {
                        return Mono.<ValidationMap>error(new AppException(
                                AppError.RULES_MAP_INVALID_DATA, "VALIDATION no activa o fuera de vigencia"));
                    }

                    final Coerced coerced;
                    try {
                        coerced = coerceValue(v.dataType(), value);
                    } catch (IllegalArgumentException iae) {
                        return Mono.<ValidationMap>error(new AppException(AppError.RULES_MAP_INVALID_DATA, iae.getMessage()));
                    }

                    // evitar duplicado exacto (NK: subtypeCode, bin, validationId)
                    return maps.findByNaturalKey(subtypeCode, bin, v.validationId())
                            .flatMap(existing -> Mono.<ValidationMap>error(new AppException(
                                    AppError.RULES_MAP_ALREADY_EXISTS,
                                    "Ya existe mapping para code=" + validationCode + " en subtype="
                                            + subtypeCode + ", bin=" + bin)))
                            .switchIfEmpty(Mono.defer(() ->
                                    tx.execute(st -> maps.save(
                                                    ValidationMap.createNew(
                                                            subtypeCode,
                                                            bin,
                                                            v.validationId(),
                                                            coerced.vf(),
                                                            coerced.vn(),
                                                            coerced.vt(),
                                                            by
                                                    )))
                                            .next()
                            ));
                });
    }

    @Override
    public Mono<ValidationMap> changeStatus(String subtypeCode, String bin, String validationCode, String newStatus, String by) {
        if (!"A".equals(newStatus) && !"I".equals(newStatus)) {
            return Mono.<ValidationMap>error(new AppException(AppError.RULES_MAP_INVALID_DATA, "status debe ser 'A' o 'I'"));
        }

        return validations.findByCode(validationCode)
                .switchIfEmpty(Mono.<com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation>error(
                        new AppException(AppError.RULES_VALIDATION_NOT_FOUND, "code=" + validationCode)))
                .flatMap(v -> maps.findByNaturalKey(subtypeCode, bin, v.validationId()))
                .switchIfEmpty(Mono.<ValidationMap>error(new AppException(
                        AppError.RULES_MAP_NOT_FOUND,
                        "No hay mapping para subtype=" + subtypeCode + ", bin=" + bin + ", code=" + validationCode)))
                .map(m -> {
                    try {
                        return m.changeStatus(newStatus, by);
                    } catch (IllegalArgumentException iae) {
                        throw new AppException(AppError.RULES_MAP_INVALID_DATA, iae.getMessage());
                    }
                })
                .flatMap(maps::save)
                .as(tx::transactional);
    }

    // ----------------- Helpers -----------------

    private record Coerced(String vf, Double vn, String vt) {}

    private static Coerced coerceValue(ValidationDataType dt, Object value) {
        switch (dt) {
            case BOOL -> {
                String vf = coerceBool(value);
                if (vf == null)
                    throw new IllegalArgumentException("value debe ser booleano (true/false/1/0) o 'SI'/'NO'");
                return new Coerced(vf, null, null);
            }
            case NUMBER -> {
                Double vn = coerceNumber(value);
                if (vn == null) throw new IllegalArgumentException("value debe ser numérico");
                return new Coerced(null, vn, null);
            }
            case TEXT -> {
                String vt = coerceText(value);
                if (vt == null || vt.isBlank()) throw new IllegalArgumentException("value debe ser texto no vacío");
                return new Coerced(null, null, vt);
            }
            default -> throw new IllegalArgumentException("dataType no soportado: " + dt);
        }
    }

    private static String coerceBool(Object value) {
        if (value instanceof Boolean b) return b ? "SI" : "NO";
        if (value instanceof Number n) {
            int i = n.intValue();
            if (i == 1) return "SI";
            if (i == 0) return "NO";
        }
        if (value instanceof String s) {
            s = s.trim().toUpperCase();
            return switch (s) {
                case "SI", "TRUE", "1" -> "SI";
                case "NO", "FALSE", "0" -> "NO";
                default -> null;
            };
        }
        return null;
    }

    private static Double coerceNumber(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try {
                return Double.valueOf(s.trim());
            } catch (NumberFormatException ignore) {}
        }
        return null;
    }

    private static String coerceText(Object value) {
        if (value == null) return null;
        return (value instanceof String s) ? s : String.valueOf(value);
    }
}

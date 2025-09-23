package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.MapRuleUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;

public record MapRuleService(
        ValidationRepository validations,
        ValidationMapRepository maps,
        SubtypeReadOnlyRepository subtypes,
        TransactionalOperator tx
) implements MapRuleUseCase {

    private static final Logger log = LoggerFactory.getLogger(MapRuleService.class);

    @Override
    public Mono<ValidationMap> attach(String subtypeCode, String bin, String validationCode, Object value, String by) {
        if (value == null) return Mono.error(new IllegalArgumentException("value es requerido"));

        var ensureSubtype = subtypes.existsByCode(subtypeCode)
                .flatMap(ok -> ok ? Mono.empty()
                        : Mono.error(new NoSuchElementException("SUBTYPE_CODE '%s' no existe".formatted(subtypeCode))));

        // Usa el que tengas disponible en tu repo:
        var ensurePair = subtypes.existsByCodeAndBinEfectivo(subtypeCode, bin) // <- si renombraste a "bin"
                // var ensurePair = subtypes.existsByCodeAndBinEfectivo(subtypeCode, bin) // <- si aún tienes este
                .flatMap(ok -> ok ? Mono.empty()
                        : Mono.error(new NoSuchElementException(
                        "BIN '%s' no existe para SUBTYPE_CODE '%s'".formatted(bin, subtypeCode))));

        var now = OffsetDateTime.now();

        return ensureSubtype.then(ensurePair)
                .then(validations.findByCode(validationCode)
                        .switchIfEmpty(Mono.error(new NoSuchElementException(
                                "VALIDATION '%s' no existe".formatted(validationCode)))))
                .flatMap(v -> {
                    boolean vigente = "A".equals(v.status())
                            && (v.validFrom() == null || !v.validFrom().isAfter(now))
                            && (v.validTo()   == null || !v.validTo().isBefore(now));
                    if (!vigente) {
                        return Mono.error(new NoSuchElementException(
                                "VALIDATION '%s' no está activa/vigente".formatted(validationCode)));
                    }

                    String vf = null;  // SI | NO (solo BOOL)
                    Double vn = null;  // solo NUMBER
                    String vt = null;  // solo TEXT

                    if (v.dataType() == ValidationDataType.BOOL) {
                        vf = coerceBool(value);
                        if (vf == null)
                            return Mono.error(new IllegalArgumentException(
                                    "value debe ser booleano (true/false/1/0) o 'SI'/'NO' para dataType=BOOL"));
                    } else if (v.dataType() == ValidationDataType.NUMBER) {
                        vn = coerceNumber(value);
                        if (vn == null)
                            return Mono.error(new IllegalArgumentException(
                                    "value debe ser numérico para dataType=NUMBER"));
                    } else if (v.dataType() == ValidationDataType.TEXT) {
                        vt = coerceText(value);
                        if (vt == null || vt.isBlank())
                            return Mono.error(new IllegalArgumentException(
                                    "value debe ser texto para dataType=TEXT"));
                    } else {
                        return Mono.error(new IllegalArgumentException("dataType no soportado: " + v.dataType()));
                    }

                    var map = ValidationMap.createNew(subtypeCode, bin, v.validationId(), vf, vn, vt, by);
                    return tx.execute(st -> maps.save(map)).next();
                });
    }

    @Override
    public Mono<ValidationMap> changeStatus(String subtypeCode, String bin, String validationCode, String newStatus, String by) {
        if (!"A".equals(newStatus) && !"I".equals(newStatus))
            return Mono.error(new IllegalArgumentException("status inválido. Use A|I"));

        return validations.findByCode(validationCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Validation no existe")))
                .flatMap(v -> maps.findByNaturalKey(subtypeCode, bin, v.validationId()))
                .switchIfEmpty(Mono.error(new NoSuchElementException("No hay mapping para esa regla")))
                .map(m -> m.changeStatus(newStatus, by))
                .flatMap(maps::save)
                .as(tx::transactional);
    }

    // ------------ helpers de coerción ------------

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
            try { return Double.valueOf(s.trim()); } catch (NumberFormatException ignore) {}
        }
        return null;
    }

    private static String coerceText(Object value) {
        if (value == null) return null;
        return (value instanceof String s) ? s : String.valueOf(value);
    }
}

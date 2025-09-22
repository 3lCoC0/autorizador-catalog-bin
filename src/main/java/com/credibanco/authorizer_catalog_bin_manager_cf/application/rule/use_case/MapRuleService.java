package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.MapRuleUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;

public record MapRuleService(ValidationRepository valRepo,
                             ValidationMapRepository mapRepo,
                             SubtypeReadOnlyRepository subtypeRepo,
                             TransactionalOperator tx) implements MapRuleUseCase {
    private static final Logger log = LoggerFactory.getLogger(MapRuleService.class);
    @Override
    public Mono<ValidationMap> attach(String subtypeCode, String binEfectivo, String validationCode, int priority, String by) {
        var now = OffsetDateTime.now();

        Mono<Void> ensureSubtype = subtypeRepo.existsByCode(subtypeCode)
                .flatMap(ok -> ok ? Mono.empty()
                        : Mono.error(new NoSuchElementException("SUBTYPE_CODE '%s' no existe".formatted(subtypeCode))));

        Mono<Void> ensurePair = subtypeRepo.existsByCodeAndBinEfectivo(subtypeCode, binEfectivo)
                .flatMap(ok -> ok ? Mono.empty()
                        : Mono.error(new NoSuchElementException(
                        "BIN_EFECTIVO '%s' no existe para SUBTYPE_CODE '%s'".formatted(binEfectivo, subtypeCode))));

        return ensureSubtype
                .then(ensurePair)
                .then(valRepo.findByCode(validationCode)
                        .switchIfEmpty(Mono.error(new NoSuchElementException("VALIDATION '%s' no existe".formatted(validationCode)))))
                .flatMap(v -> {
                    log.info("val {} status={} from={} to={} now={}",
                            validationCode, v.status(), v.validFrom(), v.validTo(), now);
                    boolean vigente = "A".equals(v.status())
                            && (v.validFrom() == null || !v.validFrom().isAfter(now))
                            && (v.validTo()   == null || !v.validTo().isBefore(now));
                    if (!vigente) {
                        return Mono.error(new NoSuchElementException("VALIDATION '%s' no está activa/vigente".formatted(validationCode)));
                    }
                    return mapRepo.existsActive(subtypeCode, binEfectivo, v.validationId())
                            .flatMap(already -> already
                                    ? Mono.error(new IllegalStateException("Ya hay una asignación activa para esa regla"))
                                    : mapRepo.save(ValidationMap.createNew(subtypeCode, binEfectivo, v.validationId(), priority, by)));
                })
                .as(tx::transactional);
    }

    @Override
    public Mono<ValidationMap> changeStatus(String subtypeCode, String binEfectivo, String validationCode, String newStatus, String by) {
        if (!"A".equals(newStatus) && !"I".equals(newStatus))
            return Mono.error(new IllegalArgumentException("status inválido"));

        return valRepo.findByCode(validationCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Validation no existe")))
                .flatMap(v -> mapRepo.findByNaturalKey(subtypeCode, binEfectivo, v.validationId()))
                .switchIfEmpty(Mono.error(new NoSuchElementException("No hay mapping para esa regla")))
                .map(m -> m.changeStatus(newStatus, by))
                .flatMap(mapRepo::save)
                .as(tx::transactional);
    }
}

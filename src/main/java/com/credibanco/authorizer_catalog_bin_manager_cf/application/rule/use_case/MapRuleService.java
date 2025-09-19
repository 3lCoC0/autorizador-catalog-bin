package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.MapRuleUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record MapRuleService(ValidationRepository valRepo,
                             ValidationMapRepository mapRepo,
                             TransactionalOperator tx) implements MapRuleUseCase {

    @Override
    public Mono<ValidationMap> attach(String subtypeCode, String binEfectivo, String validationCode, int priority, String by) {
        return valRepo.findByCode(validationCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Validation no existe")))
                .flatMap(v -> mapRepo.existsActive(subtypeCode, binEfectivo, v.validationId())
                        .flatMap(exists -> exists
                                ? Mono.error(new IllegalStateException("Ya hay una asignación activa para esa regla"))
                                : mapRepo.save(ValidationMap.createNew(subtypeCode, binEfectivo, v.validationId(), priority, by))))
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

package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.UpdatePlanUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class UpdatePlanService implements UpdatePlanUseCase {
    private final CommercePlanRepository repo;

    public UpdatePlanService(CommercePlanRepository repo) { this.repo = repo; }

    @Override
    public Mono<CommercePlan> execute(String planCode, String planName, String description, String validationMode, String updatedBy) {
        // validationMode: puede ser null si no se quiere cambiar (el trigger de DB evita cambios si ya hay ítems)
        return repo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new java.util.NoSuchElementException("Plan no encontrado")))
                .map(p -> {
                    String newMode = validationMode == null ? p.validationMode() : validationMode.toUpperCase();
                    // validar modo si viene
                    if (validationMode != null && !Objects.equals(newMode, "UNIQUE") && !Objects.equals(newMode, "MCC")) {
                        throw new IllegalArgumentException("validationMode inválido (UNIQUE | MCC)");
                    }
                    return p.updateBasics(planName, newMode, description, updatedBy);
                })
                .flatMap(repo::save);
    }
}

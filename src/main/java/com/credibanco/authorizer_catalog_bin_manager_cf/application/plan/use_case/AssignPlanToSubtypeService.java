// application/plan/use_case/AssignPlanToSubtypeService.java
package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.AssignPlanToSubtypeUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;   // ⬅️ importa esto
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public record AssignPlanToSubtypeService(CommercePlanRepository planRepo,
                                         SubtypePlanRepository subRepo,
                                         SubtypeReadOnlyRepository subtypeRepo,
                                         TransactionalOperator tx)
        implements AssignPlanToSubtypeUseCase {

    @Override
    public Mono<SubtypePlanLink> assign(String subtypeCode, String planCode, String by) {
        // 1) Validar que el SUBTYPE exista (re-uso del repo compartido)
        Mono<Void> ensureSubtype = subtypeRepo.existsByCode(subtypeCode)
                .flatMap(exists -> exists
                        ? Mono.empty()
                        : Mono.error(new NoSuchElementException("SUBTYPE no encontrado")));

        // 2) Validar plan, hacer upsert y retornar el link
        return ensureSubtype
                .then(planRepo.findByCode(planCode)
                        .switchIfEmpty(Mono.error(new NoSuchElementException("Plan no encontrado"))))
                .flatMap(p -> subRepo.upsert(subtypeCode, p.planId(), by)
                        .then(subRepo.findBySubtypeCode(subtypeCode))) // ⬅️ devuelve el link
                .as(tx::transactional);
    }
}

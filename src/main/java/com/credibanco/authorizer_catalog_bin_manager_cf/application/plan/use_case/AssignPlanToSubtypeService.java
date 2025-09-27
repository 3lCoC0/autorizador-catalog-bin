package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.AssignPlanToSubtypeUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;
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
        Mono<Void> ensureSubtype = subtypeRepo.existsByCode(subtypeCode)
                .flatMap(exists -> exists ? Mono.empty()
                        : Mono.error(new NoSuchElementException("SUBTYPE no encontrado")));

        return ensureSubtype
                .then(planRepo.findByCode(planCode)
                        .switchIfEmpty(Mono.error(new NoSuchElementException("Plan no encontrado"))))
                .flatMap(p -> subRepo.upsert(subtypeCode, p.planId(), by)
                        .then(subRepo.findBySubtype(subtypeCode)))
                .as(tx::transactional);
    }
}

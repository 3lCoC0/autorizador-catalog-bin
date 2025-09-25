package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import reactor.core.publisher.Mono;

public interface CreatePlanUseCase {
    Mono<CommercePlan> execute(String code, String name, CommerceValidationMode mode, String description, String by);
}
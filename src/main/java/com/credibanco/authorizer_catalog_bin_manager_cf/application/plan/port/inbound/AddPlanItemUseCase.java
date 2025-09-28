package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.model.PlanItemsBulkResult; // ← usa el modelo de aplicación
import reactor.core.publisher.Mono;

import java.util.List;

public interface AddPlanItemUseCase {
    Mono<PlanItem> addValue(String planCode, String value, String by);
    Mono<PlanItemsBulkResult> addMany(String planCode, List<String> values, String by);
}

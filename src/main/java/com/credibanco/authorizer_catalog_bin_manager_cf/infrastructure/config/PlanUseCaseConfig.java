package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class PlanUseCaseConfig {

    @Bean CreatePlanUseCase createPlanUseCase(CommercePlanRepository r, TransactionalOperator tx) {
        return new CreatePlanService(r, tx);
    }

    @Bean AddPlanItemUseCase addPlanItemUseCase(CommercePlanRepository pr,
                                                CommercePlanItemRepository ir,
                                                TransactionalOperator tx) {
        return new AddPlanItemService(pr, ir, tx);
    }

    @Bean AssignPlanToSubtypeUseCase assignPlanToSubtypeUseCase(CommercePlanRepository pr,
                                                                SubtypePlanRepository sr,
                                                                TransactionalOperator tx) {
        return new AssignPlanToSubtypeService(pr, sr, tx);
    }

    @Bean GetPlanUseCase getPlanUseCase(CommercePlanRepository r) { return new GetPlanService(r); }
    @Bean ListPlansUseCase listPlansUseCase(CommercePlanRepository r) { return new ListPlansService(r); }
    @Bean UpdatePlanUseCase updatePlanUseCase(CommercePlanRepository r) { return new UpdatePlanService(r); }
    @Bean ChangePlanStatusUseCase changePlanStatusUseCase(CommercePlanRepository r) { return new ChangePlanStatusService(r); }
    @Bean RemovePlanItemUseCase removePlanItemUseCase(CommercePlanRepository pr, CommercePlanItemRepository ir) { return new RemovePlanItemService(pr, ir); }
    @Bean ListPlanItemsUseCase listPlanItemsUseCase(CommercePlanRepository pr, CommercePlanItemRepository ir) { return new ListPlanItemsService(pr, ir); }
}

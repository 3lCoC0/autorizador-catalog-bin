package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.AgencyReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.IdTypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UseCaseConfigsTest {

    @Test
    void planUseCaseBeansAreInstantiated() {
        TransactionalOperator tx = mock(TransactionalOperator.class);
        CommercePlanRepository planRepo = mock(CommercePlanRepository.class);
        CommercePlanItemRepository itemRepo = mock(CommercePlanItemRepository.class);
        SubtypePlanRepository subtypePlanRepository = mock(SubtypePlanRepository.class);
        com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypeReadOnlyRepository subtypeReadOnlyRepository =
                mock(com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypeReadOnlyRepository.class);

        PlanUseCaseConfig config = new PlanUseCaseConfig();

        assertThat(config.createPlanUseCase(planRepo, tx)).isInstanceOf(CreatePlanService.class);
        assertThat(config.addPlanItemUseCase(planRepo, itemRepo, tx)).isInstanceOf(AddPlanItemService.class);
        assertThat(config.assignPlanToSubtypeUseCase(planRepo, subtypePlanRepository, subtypeReadOnlyRepository, itemRepo, tx))
                .isInstanceOf(AssignPlanToSubtypeService.class);
        assertThat(config.getPlanUseCase(planRepo)).isInstanceOf(GetPlanService.class);
        assertThat(config.listPlansUseCase(planRepo)).isInstanceOf(ListPlansService.class);
        assertThat(config.updatePlanUseCase(planRepo)).isInstanceOf(UpdatePlanService.class);
        assertThat(config.changePlanStatusUseCase(planRepo)).isInstanceOf(ChangePlanStatusService.class);
        assertThat(config.listPlanItemsUseCase(planRepo, itemRepo)).isInstanceOf(ListPlanItemsService.class);
        assertThat(config.changePlanItemStatusUseCase(planRepo, itemRepo, tx)).isInstanceOf(ChangePlanItemStatusService.class);
    }

    @Test
    void ruleUseCaseBeansAreInstantiated() {
        TransactionalOperator tx = mock(TransactionalOperator.class);
        ValidationRepository validationRepository = mock(ValidationRepository.class);
        ValidationMapRepository mapRepository = mock(ValidationMapRepository.class);
        SubtypeReadOnlyRepository subtypeRepository = mock(SubtypeReadOnlyRepository.class);

        RuleUseCaseConfig config = new RuleUseCaseConfig();

        assertThat(config.createValidationUseCase(validationRepository, tx)).isInstanceOf(CreateValidationService.class);
        assertThat(config.updateValidationUseCase(validationRepository)).isInstanceOf(UpdateValidationService.class);
        assertThat(config.changeValidationStatusUseCase(validationRepository, tx)).isInstanceOf(ChangeValidationStatusService.class);
        assertThat(config.getValidationUseCase(validationRepository)).isInstanceOf(GetValidationService.class);
        assertThat(config.listValidationsUseCase(validationRepository)).isInstanceOf(ListValidationsService.class);
        assertThat(config.mapRuleUseCase(validationRepository, mapRepository, subtypeRepository, tx)).isInstanceOf(MapRuleService.class);
        assertThat(config.listRulesForSubtypeUseCase(mapRepository)).isInstanceOf(ListRulesForSubtypeService.class);
    }

    @Test
    void subtypeUseCaseBeansAreInstantiated() {
        TransactionalOperator tx = mock(TransactionalOperator.class);
        SubtypeRepository subtypeRepository = mock(SubtypeRepository.class);
        BinReadOnlyRepository binRepository = mock(BinReadOnlyRepository.class);
        IdTypeReadOnlyRepository idTypeRepository = mock(IdTypeReadOnlyRepository.class);
        AgencyReadOnlyRepository agencyRepository = mock(AgencyReadOnlyRepository.class);

        SubtypeUseCaseConfig config = new SubtypeUseCaseConfig();

        assertThat(config.createSubtypeUseCase(subtypeRepository, binRepository, idTypeRepository, tx)).isInstanceOf(CreateSubtypeService.class);
        assertThat(config.updateSubtypeBasicsUseCase(subtypeRepository, binRepository, idTypeRepository, tx)).isInstanceOf(UpdateSubtypeBasicsService.class);
        assertThat(config.changeSubtypeStatusUseCase(subtypeRepository, agencyRepository, tx)).isInstanceOf(ChangeSubtypeStatusService.class);
        assertThat(config.getSubtypeUseCase(subtypeRepository)).isInstanceOf(GetSubtypeService.class);
        assertThat(config.listSubtypesUseCase(subtypeRepository, binRepository)).isInstanceOf(ListSubtypesService.class);
    }
}

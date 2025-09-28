package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class RuleUseCaseConfig {
    @Bean
    CreateValidationUseCase createValidationUseCase(ValidationRepository r, TransactionalOperator tx) {
        return new CreateValidationService(r, tx);
    }
    @Bean UpdateValidationUseCase updateValidationUseCase(ValidationRepository r) { return new UpdateValidationService(r); }
    @Bean ChangeValidationStatusUseCase changeValidationStatusUseCase(ValidationRepository r, TransactionalOperator tx) { return new ChangeValidationStatusService(r, tx); }
    @Bean GetValidationUseCase getValidationUseCase(ValidationRepository r) { return new GetValidationService(r); }
    @Bean ListValidationsUseCase listValidationsUseCase(ValidationRepository r) { return new ListValidationsService(r); }

    @Bean
    MapRuleUseCase mapRuleUseCase(ValidationRepository vr,
                                  ValidationMapRepository mr,
                                  SubtypeReadOnlyRepository sr,
                                  TransactionalOperator tx) {
        return new MapRuleService(vr, mr, sr, tx);
    }

    @Bean
    ListRulesForSubtypeUseCase listRulesForSubtypeUseCase(ValidationMapRepository mr) {
        return new ListRulesForSubtypeService(mr);
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class AgencyUseCaseConfig {

    @Bean
    public CreateAgencyUseCase createAgencyUseCase(
            AgencyRepository repo, SubtypeReadOnlyRepository subtypeRepo, TransactionalOperator tx
    ) { return new CreateAgencyService(repo, subtypeRepo, tx); }

    @Bean
    public UpdateAgencyUseCase updateAgencyUseCase(
            AgencyRepository repo,
            SubtypeReadOnlyRepository subtypeRepo,
            TransactionalOperator tx
    ) {
        return new UpdateAgencyService(repo, subtypeRepo, tx);
    }


    @Bean
    public ChangeAgencyStatusUseCase changeAgencyStatusUseCase(
            AgencyRepository repo, SubtypeReadOnlyRepository subtypeRepo, TransactionalOperator tx
    ) { return new ChangeAgencyStatusService(repo, subtypeRepo, tx); }

    @Bean
    public GetAgencyUseCase getAgencyUseCase(
            AgencyRepository repo,
            SubtypeReadOnlyRepository subtypeRepo
    ) {
        return new GetAgencyService(repo, subtypeRepo);
    }


    @Bean
    public ListAgenciesUseCase listAgenciesUseCase(
            AgencyRepository repo,
            SubtypeReadOnlyRepository subtypeRepo
    ) {
        return new ListAgenciesService(repo, subtypeRepo);
    }
}

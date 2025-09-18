package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.AgencyReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.IdTypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class SubtypeUseCaseConfig {

    @Bean
    public CreateSubtypeUseCase createSubtypeUseCase(
            SubtypeRepository repo,
            BinReadOnlyRepository binRepo,
            IdTypeReadOnlyRepository idTypeRepo,
            TransactionalOperator tx
    ) { return new CreateSubtypeService(repo, binRepo, idTypeRepo, tx); }

    @Bean
    public UpdateSubtypeBasicsUseCase updateSubtypeBasicsUseCase(
            SubtypeRepository repo, TransactionalOperator tx
    ) { return new UpdateSubtypeBasicsService(repo, tx); }

    @Bean
    public ChangeSubtypeStatusUseCase changeSubtypeStatusUseCase(
            SubtypeRepository repo,
            AgencyReadOnlyRepository agencyRepo,
            TransactionalOperator tx
    ) { return new ChangeSubtypeStatusService(repo, agencyRepo, tx); }

    @Bean
    public GetSubtypeUseCase getSubtypeUseCase(SubtypeRepository repo) {
        return new GetSubtypeService(repo);
    }

    @Bean
    public ListSubtypesUseCase listSubtypesUseCase(SubtypeRepository repo) {
        return new ListSubtypesService(repo);
    }
}
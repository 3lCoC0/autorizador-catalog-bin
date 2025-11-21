package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AgencyUseCaseConfigTest {

    private AgencyUseCaseConfig config;
    private AgencyRepository agencyRepository;
    private SubtypeReadOnlyRepository subtypeRepository;
    private TransactionalOperator transactionalOperator;

    @BeforeEach
    void setUp() {
        config = new AgencyUseCaseConfig();
        agencyRepository = mock(AgencyRepository.class);
        subtypeRepository = mock(SubtypeReadOnlyRepository.class);
        transactionalOperator = mock(TransactionalOperator.class);
    }

    @Test
    void createAgencyUseCaseCreatesServiceWithDependencies() {
        CreateAgencyUseCase useCase = config.createAgencyUseCase(agencyRepository, subtypeRepository, transactionalOperator);
        assertInstanceOf(CreateAgencyService.class, useCase);
        CreateAgencyService service = (CreateAgencyService) useCase;
        assertSame(agencyRepository, service.repo());
        assertSame(subtypeRepository, service.subtypeRepo());
        assertSame(transactionalOperator, service.tx());
    }

    @Test
    void updateAgencyUseCaseCreatesServiceWithDependencies() {
        UpdateAgencyUseCase useCase = config.updateAgencyUseCase(agencyRepository, subtypeRepository, transactionalOperator);
        assertInstanceOf(UpdateAgencyService.class, useCase);
        UpdateAgencyService service = (UpdateAgencyService) useCase;
        assertSame(agencyRepository, service.repo());
        assertSame(subtypeRepository, service.subtypeRepo());
        assertSame(transactionalOperator, service.tx());
    }

    @Test
    void changeAgencyStatusUseCaseCreatesServiceWithDependencies() {
        ChangeAgencyStatusUseCase useCase = config.changeAgencyStatusUseCase(agencyRepository, subtypeRepository, transactionalOperator);
        assertInstanceOf(ChangeAgencyStatusService.class, useCase);
        ChangeAgencyStatusService service = (ChangeAgencyStatusService) useCase;
        assertSame(agencyRepository, service.repo());
        assertSame(subtypeRepository, service.subtypeRepo());
        assertSame(transactionalOperator, service.tx());
    }

    @Test
    void getAgencyUseCaseCreatesServiceWithDependencies() {
        GetAgencyUseCase useCase = config.getAgencyUseCase(agencyRepository, subtypeRepository);
        assertInstanceOf(GetAgencyService.class, useCase);
        GetAgencyService service = (GetAgencyService) useCase;
        assertSame(agencyRepository, service.repo());
        assertSame(subtypeRepository, service.subtypeRepo());
    }

    @Test
    void listAgenciesUseCaseCreatesServiceWithDependencies() {
        ListAgenciesUseCase useCase = config.listAgenciesUseCase(agencyRepository, subtypeRepository);
        assertInstanceOf(ListAgenciesService.class, useCase);
        ListAgenciesService service = (ListAgenciesService) useCase;
        assertSame(agencyRepository, service.repo());
        assertSame(subtypeRepository, service.subtypeRepo());
    }
}

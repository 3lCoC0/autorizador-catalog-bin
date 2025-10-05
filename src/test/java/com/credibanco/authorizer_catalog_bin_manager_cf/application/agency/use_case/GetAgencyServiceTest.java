package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetAgencyServiceTest {

    private AgencyRepository repo;
    private SubtypeReadOnlyRepository subtypeRepo;
    private GetAgencyService service;

    @BeforeEach
    void setUp() {
        repo = mock(AgencyRepository.class);
        subtypeRepo = mock(SubtypeReadOnlyRepository.class);
        service = new GetAgencyService(repo, subtypeRepo);
    }

    @Test
    void whenSubtypeMissingThenEmitSubtypeNotFound() {
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute("01", "001"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(subtypeRepo).existsByCode("01");
        verifyNoInteractions(repo);
    }

    @Test
    void whenAgencyMissingThenEmitNotFound() {
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(true));
        when(repo.findByPk("01", "001")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("01", "001"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.AGENCY_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(subtypeRepo).existsByCode("01");
        verify(repo).findByPk("01", "001");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void whenAgencyExistsThenReturnIt() {
        Agency agency = Agency.rehydrate("01", "001", "Agency", null, null, null, null,
                null, null, null, null, null, null, null, null, null, "A", null, null, null);
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(true));
        when(repo.findByPk("01", "001")).thenReturn(Mono.just(agency));

        StepVerifier.create(service.execute("01", "001"))
                .expectNext(agency)
                .verifyComplete();

        verify(subtypeRepo).existsByCode("01");
        verify(repo).findByPk("01", "001");
        verifyNoMoreInteractions(repo);
    }
}

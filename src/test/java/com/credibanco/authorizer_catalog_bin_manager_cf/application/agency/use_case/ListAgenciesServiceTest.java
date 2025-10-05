package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ListAgenciesServiceTest {

    private AgencyRepository repo;
    private SubtypeReadOnlyRepository subtypeRepo;
    private ListAgenciesService service;

    @BeforeEach
    void setUp() {
        repo = mock(AgencyRepository.class);
        subtypeRepo = mock(SubtypeReadOnlyRepository.class);
        service = new ListAgenciesService(repo, subtypeRepo);
    }

    @Test
    void whenPaginationInvalidThenEmitInvalidData() {
        StepVerifier.create(service.execute("01", null, null, -1, 0))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.AGENCY_INVALID_DATA, appException.getError());
                    assertTrue(appException.getMessage().contains("page"));
                })
                .verify();

        verifyNoInteractions(repo, subtypeRepo);
    }

    @Test
    void whenSubtypeNotProvidedThenListFromRepository() {
        Agency agency = Agency.rehydrate("01", "001", "Agency", null, null, null, null,
                null, null, null, null, null, null, null, null, null, "A", null, null, null);
        when(repo.findAll(null, "A", "ag", 0, 20)).thenReturn(Flux.just(agency));

        StepVerifier.create(service.execute(null, "A", "ag", 0, 20))
                .expectNext(agency)
                .verifyComplete();

        verify(repo).findAll(null, "A", "ag", 0, 20);
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(subtypeRepo);
    }

    @Test
    void whenSubtypeNotFoundThenEmitError() {
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute("01", "A", null, 0, 10))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(subtypeRepo).existsByCode("01");
        verifyNoInteractions(repo);
    }

    @Test
    void whenSubtypeExistsThenDelegateToRepository() {
        Agency agency = Agency.rehydrate("01", "001", "Agency", null, null, null, null,
                null, null, null, null, null, null, null, null, null, "A", null, null, null);
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(true));
        when(repo.findAll("01", "I", null, 1, 5)).thenReturn(Flux.just(agency));

        StepVerifier.create(service.execute("01", "I", null, 1, 5))
                .expectNext(agency)
                .verifyComplete();

        verify(subtypeRepo).existsByCode("01");
        verify(repo).findAll("01", "I", null, 1, 5);
        verifyNoMoreInteractions(repo);
    }
}

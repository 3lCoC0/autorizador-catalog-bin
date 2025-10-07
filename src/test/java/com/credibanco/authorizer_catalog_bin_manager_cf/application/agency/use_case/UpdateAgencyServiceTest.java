package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UpdateAgencyServiceTest {

    private AgencyRepository repo;
    private SubtypeReadOnlyRepository subtypeRepo;
    private TransactionalOperator txOperator;
    private UpdateAgencyService service;

    @BeforeEach
    void setUp() {
        repo = mock(AgencyRepository.class);
        subtypeRepo = mock(SubtypeReadOnlyRepository.class);
        txOperator = mock(TransactionalOperator.class);
        service = new UpdateAgencyService(repo, subtypeRepo, txOperator);

        when(txOperator.transactional(any(Publisher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void whenSubtypeDoesNotExistThenEmitSubtypeNotFound() {
        Agency updated = Agency.rehydrate("01", "001", "Agency", null, null, null, null,
                null, null, null, null, null, null, null, null, null, "A", null, null, "tester");
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute(updated))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(subtypeRepo).existsByCode("01");
        verifyNoInteractions(repo);
    }

    @Test
    void whenAgencyDoesNotExistThenEmitNotFound() {
        Agency updated = Agency.rehydrate("01", "001", "Agency", null, null, null, null,
                null, null, null, null, null, null, null, null, null, "A", null, null, "tester");
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(true));
        when(repo.findByPk("01", "001")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute(updated))
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
    void whenDomainValidationFailsThenEmitInvalidData() {
        Agency updated = Agency.rehydrate("01", "001", "Updated", null, null, null, null,
                null, null, null, null, null, null, null, null, null, "A", null, null, "tester");
        Agency current = spy(Agency.rehydrate("01", "001", "Current", null, null, null, null,
                null, null, null, null, null, null, null, null, null, "A", null, null, "admin"));

        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(true));
        when(repo.findByPk("01", "001")).thenReturn(Mono.just(current));
        doThrow(new IllegalArgumentException("name es requerido"))
                .when(current).updateBasics(
                        anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        StepVerifier.create(service.execute(updated))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.AGENCY_INVALID_DATA, appException.getError());
                    assertEquals("name es requerido", appException.getMessage());
                })
                .verify();

        verify(subtypeRepo).existsByCode("01");
        verify(repo).findByPk("01", "001");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void whenUpdateSucceedsThenPersistAndReturn() {
        Agency updated = Agency.rehydrate("01", "001", "Agency", null, null, null, null,
                null, null, null, null, null, null, null, null, null, "A", null, null, "tester");
        Agency current = Agency.rehydrate("01", "001", "Current", null, null, null, null,
                null, null, null, null, null, null, null, null, null, "A", null, null, "admin");
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(true));
        when(repo.findByPk("01", "001")).thenReturn(Mono.just(current));
        when(repo.save(any(Agency.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute(updated))
                .assertNext(saved -> {
                    assertEquals("Agency", saved.name());
                    assertEquals("tester", saved.updatedBy());
                })
                .verifyComplete();

        verify(subtypeRepo).existsByCode("01");
        verify(repo).findByPk("01", "001");
        verify(repo).save(any(Agency.class));
        verifyNoMoreInteractions(repo);
    }
}

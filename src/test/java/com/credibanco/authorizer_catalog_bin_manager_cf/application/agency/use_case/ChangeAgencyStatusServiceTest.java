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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChangeAgencyStatusServiceTest {

    private AgencyRepository repo;
    private SubtypeReadOnlyRepository subtypeRepo;
    private TransactionalOperator txOperator;
    private ChangeAgencyStatusService service;

    @BeforeEach
    void setUp() {
        repo = mock(AgencyRepository.class);
        subtypeRepo = mock(SubtypeReadOnlyRepository.class);
        txOperator = mock(TransactionalOperator.class);
        service = new ChangeAgencyStatusService(repo, subtypeRepo, txOperator);

        when(txOperator.transactional(any(Publisher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void whenStatusInvalidThenEmitInvalidData() {
        StepVerifier.create(service.execute("01", "001", "X", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.AGENCY_INVALID_DATA, appException.getError());
                    assertTrue(appException.getMessage().contains("status"));
                })
                .verify();

        verifyNoInteractions(repo, subtypeRepo);
    }

    @Test
    void whenAgencyMissingThenEmitNotFound() {
        when(repo.findByPk("01", "001")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("01", "001", "A", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.AGENCY_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(repo).findByPk("01", "001");
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(subtypeRepo);
    }

    @Test
    void whenSubtypeMissingThenEmitSubtypeNotFound() {
        Agency current = Agency.rehydrate("01", "001", "Agency", null, null, null, null,
                null, null, null, null, null, null, null, null, null, "A", null, null, null);
        when(repo.findByPk("01", "001")).thenReturn(Mono.just(current));
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute("01", "001", "A", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(repo).findByPk("01", "001");
        verify(subtypeRepo).existsByCode("01");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void whenInactivatingOnlyActiveAgencyThenEmitConflict() {
        Agency current = Agency.rehydrate("01", "001", "Agency", null, null, null, null,
                null, null, null, null, null, null, null, null, null, "A", null, null, null);
        when(repo.findByPk("01", "001")).thenReturn(Mono.just(current));
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(true));
        when(repo.existsAnotherActive("01", "001")).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute("01", "001", "I", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.AGENCY_CONFLICT_RULE, appException.getError());
                    assertTrue(appException.getMessage().contains("única"));
                })
                .verify();

        verify(repo).findByPk("01", "001");
        verify(subtypeRepo).existsByCode("01");
        verify(repo).existsAnotherActive("01", "001");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void whenSaveFailsWithIllegalArgumentThenWrapInAppException() {
        Agency current = Agency.rehydrate("01", "001", "Agency", null, null, null, null,
                null, null, null, null, null, null, null, null, null, "I", null, null, null);
        when(repo.findByPk("01", "001")).thenReturn(Mono.just(current));
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(true));
        when(repo.save(any(Agency.class))).thenReturn(Mono.error(new IllegalArgumentException("invalid status")));

        StepVerifier.create(service.execute("01", "001", "A", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.AGENCY_INVALID_DATA, appException.getError());
                    assertEquals("invalid status", appException.getMessage());
                })
                .verify();

        verify(repo).findByPk("01", "001");
        verify(subtypeRepo).existsByCode("01");
        verify(repo).save(any(Agency.class));
    }

    @Test
    void whenChangeStatusSucceedsThenPersistAndReturn() {
        Agency current = Agency.rehydrate("01", "001", "Agency", null, null, null, null,
                null, null, null, null, null, null, null, null, null, "A", null, null, null);
        when(repo.findByPk("01", "001")).thenReturn(Mono.just(current));
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(true));
        when(repo.existsAnotherActive("01", "001")).thenReturn(Mono.just(true));
        when(repo.save(any(Agency.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute("01", "001", "I", "tester"))
                .assertNext(saved -> {
                    assertEquals("I", saved.status());
                    assertEquals("tester", saved.updatedBy());
                })
                .verifyComplete();

        verify(repo).findByPk("01", "001");
        verify(subtypeRepo).existsByCode("01");
        verify(repo).existsAnotherActive("01", "001");
        verify(repo).save(any(Agency.class));
    }
}

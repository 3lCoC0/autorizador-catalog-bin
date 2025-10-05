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
import static org.mockito.Mockito.*;

class CreateAgencyServiceTest {

    private AgencyRepository repo;
    private SubtypeReadOnlyRepository subtypeRepo;
    private TransactionalOperator txOperator;
    private CreateAgencyService service;

    @BeforeEach
    void setUp() {
        repo = mock(AgencyRepository.class);
        subtypeRepo = mock(SubtypeReadOnlyRepository.class);
        txOperator = mock(TransactionalOperator.class);
        service = new CreateAgencyService(repo, subtypeRepo, txOperator);

        when(txOperator.transactional(any(Publisher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void whenSubtypeDoesNotExistThenEmitSubtypeNotFound() {
        Agency draft = Agency.createNew("01", "001", "Agency", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, "tester");
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute(draft))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.SUBTYPE_NOT_FOUND, appException.getError());
                    assertTrue(appException.getMessage().contains("subtypeCode=01"));
                })
                .verify();

        verify(subtypeRepo).existsByCode("01");
        verifyNoInteractions(repo);
    }

    @Test
    void whenAgencyAlreadyExistsThenEmitConflictError() {
        Agency draft = Agency.createNew("01", "001", "Agency", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, "tester");
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(true));
        when(repo.existsByPk("01", "001")).thenReturn(Mono.just(true));

        StepVerifier.create(service.execute(draft))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.AGENCY_ALREADY_EXISTS, ((AppException) error).getError());
                })
                .verify();

        verify(subtypeRepo).existsByCode("01");
        verify(repo).existsByPk("01", "001");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void whenSaveFailsWithIllegalArgumentThenWrapInAppException() {
        Agency draft = Agency.createNew("01", "001", "Agency", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, "tester");
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(true));
        when(repo.existsByPk("01", "001")).thenReturn(Mono.just(false));
        when(repo.save(draft)).thenReturn(Mono.error(new IllegalArgumentException("invalid")));

        StepVerifier.create(service.execute(draft))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.AGENCY_INVALID_DATA, appException.getError());
                    assertEquals("invalid", appException.getMessage());
                })
                .verify();

        verify(subtypeRepo).existsByCode("01");
        verify(repo).existsByPk("01", "001");
        verify(repo).save(draft);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void whenDraftValidThenSaveAndReturnAgency() {
        Agency draft = Agency.createNew("01", "001", "Agency", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, "tester");
        when(subtypeRepo.existsByCode("01")).thenReturn(Mono.just(true));
        when(repo.existsByPk("01", "001")).thenReturn(Mono.just(false));
        when(repo.save(draft)).thenReturn(Mono.just(draft));

        StepVerifier.create(service.execute(draft))
                .expectNext(draft)
                .verifyComplete();

        verify(subtypeRepo).existsByCode("01");
        verify(repo).existsByPk("01", "001");
        verify(repo).save(draft);
        verifyNoMoreInteractions(repo);
    }
}

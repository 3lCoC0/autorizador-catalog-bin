package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChangeValidationStatusServiceTest {

    private ValidationRepository repository;
    private TransactionalOperator tx;
    private ChangeValidationStatusService service;

    @BeforeEach
    void setUp() {
        repository = mock(ValidationRepository.class);
        tx = mock(TransactionalOperator.class);
        service = new ChangeValidationStatusService(repository, tx);

        when(tx.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void whenStatusInvalidReturnsAppException() {
        StepVerifier.create(service.execute("CODE", "X", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_VALIDATION_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void whenValidationNotFoundReturnsError() {
        when(repository.findByCode("CODE")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("CODE", "A", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_VALIDATION_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(repository).findByCode("CODE");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenDomainRejectsStatusReturnsAppException() {
        Validation current = mock(Validation.class);
        when(repository.findByCode("CODE")).thenReturn(Mono.just(current));
        when(current.changeStatus("A", "user"))
                .thenThrow(new IllegalArgumentException("bad"));

        StepVerifier.create(service.execute("CODE", "A", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_VALIDATION_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();

        verify(repository).findByCode("CODE");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenStatusValidSavesEntity() {
        Validation current = Validation.createNew("CODE", "desc", ValidationDataType.TEXT, "creator");
        when(repository.findByCode("CODE")).thenReturn(Mono.just(current));
        when(repository.save(any(Validation.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute("CODE", "I", "user"))
                .assertNext(updated -> {
                    assertEquals("I", updated.status());
                    assertEquals("user", updated.updatedBy());
                })
                .verifyComplete();

        verify(repository).findByCode("CODE");
        verify(repository).save(any(Validation.class));
        verifyNoMoreInteractions(repository);
    }
}


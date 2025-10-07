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

class CreateValidationServiceTest {

    private ValidationRepository repository;
    private TransactionalOperator tx;
    private CreateValidationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ValidationRepository.class);
        tx = mock(TransactionalOperator.class);
        service = new CreateValidationService(repository, tx);

        when(tx.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void whenValidationAlreadyExistsReturnsError() {
        when(repository.existsByCode("CODE")).thenReturn(Mono.just(true));

        StepVerifier.create(service.execute("CODE", "desc", ValidationDataType.BOOL, "me"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_VALIDATION_ALREADY_EXISTS, ((AppException) error).getError());
                })
                .verify();

        verify(repository).existsByCode("CODE");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenDomainRejectsDataReturnsAppException() {
        when(repository.existsByCode(" ")).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute(" ", "desc", ValidationDataType.TEXT, "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_VALIDATION_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();

        verify(repository).existsByCode(" ");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenValidDataPersistsValidation() {
        when(repository.existsByCode("CODE")).thenReturn(Mono.just(false));
        when(repository.save(any(Validation.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute("CODE", "desc", ValidationDataType.NUMBER, "user"))
                .assertNext(validation -> {
                    assertEquals("CODE", validation.code());
                    assertEquals(ValidationDataType.NUMBER, validation.dataType());
                })
                .verifyComplete();

        verify(repository).existsByCode("CODE");
        verify(repository).save(any(Validation.class));
        verifyNoMoreInteractions(repository);
    }
}


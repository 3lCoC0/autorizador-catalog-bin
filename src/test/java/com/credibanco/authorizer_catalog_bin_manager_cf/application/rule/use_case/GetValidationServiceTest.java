package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class GetValidationServiceTest {

    private ValidationRepository repository;
    private GetValidationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ValidationRepository.class);
        service = new GetValidationService(repository);
    }

    @Test
    void whenValidationMissingThrowsException() {
        when(repository.findByCode("VAL")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("VAL"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_VALIDATION_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(repository).findByCode("VAL");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenValidationExistsReturnsIt() {
        Validation validation = Validation.createNew("VAL", "desc", ValidationDataType.BOOL, "tester");
        when(repository.findByCode("VAL")).thenReturn(Mono.just(validation));

        StepVerifier.create(service.execute("VAL"))
                .expectNext(validation)
                .verifyComplete();

        verify(repository).findByCode("VAL");
        verifyNoMoreInteractions(repository);
    }
}


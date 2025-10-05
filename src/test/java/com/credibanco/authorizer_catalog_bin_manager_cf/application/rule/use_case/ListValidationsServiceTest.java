package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ListValidationsServiceTest {

    private ValidationRepository repository;
    private ListValidationsService service;

    @BeforeEach
    void setUp() {
        repository = mock(ValidationRepository.class);
        service = new ListValidationsService(repository);
    }

    @Test
    void whenPaginationInvalidReturnsError() {
        StepVerifier.create(service.execute("A", null, -1, 10))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_VALIDATION_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();

        StepVerifier.create(service.execute("A", null, 0, 0))
                .expectError(AppException.class)
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void whenPaginationValidDelegatesToRepository() {
        Validation validation = Validation.createNew("V1", "desc", ValidationDataType.TEXT, "tester");
        when(repository.findAll("A", "search", 1, 5)).thenReturn(Flux.just(validation));

        StepVerifier.create(service.execute("A", "search", 1, 5))
                .expectNext(validation)
                .verifyComplete();

        verify(repository).findAll("A", "search", 1, 5);
        verifyNoMoreInteractions(repository);
    }
}


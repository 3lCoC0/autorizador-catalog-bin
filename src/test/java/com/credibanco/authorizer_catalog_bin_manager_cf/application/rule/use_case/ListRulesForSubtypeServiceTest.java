package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ListRulesForSubtypeServiceTest {

    private ValidationMapRepository mapRepository;
    private ListRulesForSubtypeService service;

    @BeforeEach
    void setUp() {
        mapRepository = mock(ValidationMapRepository.class);
        service = new ListRulesForSubtypeService(mapRepository);
    }

    @Test
    void whenPaginationInvalidReturnsError() {
        StepVerifier.create(service.execute("SUB", "BIN", "A", -1, 10))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_MAP_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();

        StepVerifier.create(service.execute("SUB", "BIN", "A", 0, 0))
                .expectError(AppException.class)
                .verify();

        verifyNoInteractions(mapRepository);
    }

    @Test
    void whenStatusBlankReplacedWithNull() {
        ValidationMap map = ValidationMap.createNew("SUB", "BIN", 1L, "SI", null, null, "user");
        when(mapRepository.findResolved("SUB", "BIN", null, 0, 10)).thenReturn(Flux.just(map));

        StepVerifier.create(service.execute("SUB", "BIN", " ", 0, 10))
                .expectNext(map)
                .verifyComplete();

        verify(mapRepository).findResolved("SUB", "BIN", null, 0, 10);
        verifyNoMoreInteractions(mapRepository);
    }

    @Test
    void whenStatusProvidedDelegatesDirectly() {
        when(mapRepository.findResolved("SUB", null, "I", 2, 20)).thenReturn(Flux.empty());

        StepVerifier.create(service.execute("SUB", null, "I", 2, 20))
                .verifyComplete();

        verify(mapRepository).findResolved("SUB", null, "I", 2, 20);
        verifyNoMoreInteractions(mapRepository);
    }
}


package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MapRuleServiceTest {

    private ValidationRepository validations;
    private ValidationMapRepository maps;
    private SubtypeReadOnlyRepository subtypes;
    private TransactionalOperator tx;
    private MapRuleService service;

    @BeforeEach
    void setUp() {
        validations = mock(ValidationRepository.class);
        maps = mock(ValidationMapRepository.class);
        subtypes = mock(SubtypeReadOnlyRepository.class);
        tx = mock(TransactionalOperator.class);
        service = new MapRuleService(validations, maps, subtypes, tx);

        when(tx.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tx.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<ReactiveTransaction, Mono<ValidationMap>> callback = invocation.getArgument(0);
            return Mono.defer(() -> callback.apply(null));
        });
    }

    @Test
    void whenValueNullReturnsError() {
        StepVerifier.create(service.attach("S", "B", "VAL", null, "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_MAP_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();

        verifyNoInteractions(subtypes, validations, maps);
    }

    @Test
    void whenSubtypeMissingReturnsError() {
        when(subtypes.existsByCode("S")).thenReturn(Mono.just(false));

        StepVerifier.create(service.attach("S", "B", "VAL", "SI", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(subtypes).existsByCode("S");
        verifyNoMoreInteractions(subtypes, validations, maps);
    }

    @Test
    void whenBinPairMissingReturnsError() {
        when(subtypes.existsByCode("S")).thenReturn(Mono.just(true));
        when(subtypes.existsByCodeAndBinEfectivo("S", "B")).thenReturn(Mono.just(false));

        StepVerifier.create(service.attach("S", "B", "VAL", "SI", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.BIN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(subtypes).existsByCode("S");
        verify(subtypes).existsByCodeAndBinEfectivo("S", "B");
        verifyNoMoreInteractions(subtypes);
        verifyNoInteractions(validations, maps);
    }

    @Test
    void whenValidationMissingReturnsError() {
        activeSubtype();
        when(validations.findByCode("VAL")).thenReturn(Mono.empty());

        StepVerifier.create(service.attach("S", "B", "VAL", "SI", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_VALIDATION_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(validations).findByCode("VAL");
        verifyNoMoreInteractions(validations);
    }

    @Test
    void whenValidationNotActiveReturnsError() {
        activeSubtype();
        Validation validation = Validation.rehydrate(1L, "VAL", "desc", ValidationDataType.BOOL,
                "I", null, null, OffsetDateTime.now(), OffsetDateTime.now(), "user");
        when(validations.findByCode("VAL")).thenReturn(Mono.just(validation));

        StepVerifier.create(service.attach("S", "B", "VAL", "SI", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_MAP_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void whenValueCannotBeCoercedReturnsError() {
        activeSubtype();
        Validation validation = activeValidation(ValidationDataType.BOOL);
        when(validations.findByCode("VAL")).thenReturn(Mono.just(validation));

        StepVerifier.create(service.attach("S", "B", "VAL", "MAYBE", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_MAP_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void whenMappingAlreadyExistsReturnsError() {
        activeSubtype();
        Validation validation = activeValidation(ValidationDataType.TEXT);
        when(validations.findByCode("VAL")).thenReturn(Mono.just(validation));
        ValidationMap existing = ValidationMap.createNew("S", "B", 1L, null, null, "value", "user");
        when(maps.findByNaturalKey("S", "B", validation.validationId()))
                .thenReturn(Mono.just(existing));

        StepVerifier.create(service.attach("S", "B", "VAL", "text", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_MAP_ALREADY_EXISTS, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void whenBoolValueValidSavesMapping() {
        activeSubtype();
        Validation validation = activeValidation(ValidationDataType.BOOL);
        when(validations.findByCode("VAL")).thenReturn(Mono.just(validation));
        when(maps.findByNaturalKey("S", "B", validation.validationId())).thenReturn(Mono.empty());
        when(maps.save(any(ValidationMap.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.attach("S", "B", "VAL", "true", "user"))
                .assertNext(saved -> {
                    assertEquals("SI", saved.valueFlag());
                    assertNull(saved.valueNum());
                    assertNull(saved.valueText());
                })
                .verifyComplete();

        verify(maps).findByNaturalKey("S", "B", validation.validationId());
        verify(maps).save(any(ValidationMap.class));
    }

    @Test
    void whenNumberValueValidSavesMapping() {
        activeSubtype();
        Validation validation = activeValidation(ValidationDataType.NUMBER);
        when(validations.findByCode("VAL")).thenReturn(Mono.just(validation));
        when(maps.findByNaturalKey("S", "B", validation.validationId())).thenReturn(Mono.empty());
        when(maps.save(any(ValidationMap.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.attach("S", "B", "VAL", "123.5", "user"))
                .assertNext(saved -> {
                    assertEquals(123.5, saved.valueNum());
                    assertNull(saved.valueFlag());
                    assertNull(saved.valueText());
                })
                .verifyComplete();
    }

    @Test
    void whenNumberCannotBeCoercedReturnsError() {
        activeSubtype();
        Validation validation = activeValidation(ValidationDataType.NUMBER);
        when(validations.findByCode("VAL")).thenReturn(Mono.just(validation));

        StepVerifier.create(service.attach("S", "B", "VAL", "abc", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_MAP_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void whenTextValueValidSavesMapping() {
        activeSubtype();
        Validation validation = activeValidation(ValidationDataType.TEXT);
        when(validations.findByCode("VAL")).thenReturn(Mono.just(validation));
        when(maps.findByNaturalKey("S", "B", validation.validationId())).thenReturn(Mono.empty());
        when(maps.save(any(ValidationMap.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.attach("S", "B", "VAL", 42, "user"))
                .assertNext(saved -> {
                    assertEquals("42", saved.valueText());
                    assertNull(saved.valueFlag());
                    assertNull(saved.valueNum());
                })
                .verifyComplete();
    }

    @Test
    void whenTextBlankReturnsError() {
        activeSubtype();
        Validation validation = activeValidation(ValidationDataType.TEXT);
        when(validations.findByCode("VAL")).thenReturn(Mono.just(validation));

        StepVerifier.create(service.attach("S", "B", "VAL", " ", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_MAP_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void changeStatusWhenStatusInvalidReturnsError() {
        StepVerifier.create(service.changeStatus("S", "B", "VAL", "X", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_MAP_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();

        verifyNoInteractions(validations, maps);
    }

    @Test
    void changeStatusWhenValidationMissingReturnsError() {
        when(validations.findByCode("VAL")).thenReturn(Mono.empty());

        StepVerifier.create(service.changeStatus("S", "B", "VAL", "A", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_VALIDATION_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void changeStatusWhenMappingMissingReturnsError() {
        Validation validation = activeValidation(ValidationDataType.BOOL);
        when(validations.findByCode("VAL")).thenReturn(Mono.just(validation));
        when(maps.findByNaturalKey("S", "B", validation.validationId())).thenReturn(Mono.empty());

        StepVerifier.create(service.changeStatus("S", "B", "VAL", "A", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_MAP_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void changeStatusWhenDomainRejectsStatusReturnsError() {
        Validation validation = activeValidation(ValidationDataType.TEXT);
        when(validations.findByCode("VAL")).thenReturn(Mono.just(validation));
        ValidationMap mapping = mock(ValidationMap.class);
        when(maps.findByNaturalKey("S", "B", validation.validationId())).thenReturn(Mono.just(mapping));
        when(mapping.changeStatus("I", "user")).thenThrow(new IllegalArgumentException("bad"));

        StepVerifier.create(service.changeStatus("S", "B", "VAL", "I", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.RULES_MAP_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void changeStatusWhenValidSavesMapping() {
        Validation validation = activeValidation(ValidationDataType.BOOL);
        when(validations.findByCode("VAL")).thenReturn(Mono.just(validation));
        ValidationMap mapping = ValidationMap.createNew("S", "B", validation.validationId(), "SI", null, null, "user");
        when(maps.findByNaturalKey("S", "B", validation.validationId())).thenReturn(Mono.just(mapping));
        when(maps.save(any(ValidationMap.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.changeStatus("S", "B", "VAL", "I", "user"))
                .assertNext(updated -> {
                    assertEquals("I", updated.status());
                    assertEquals("user", updated.updatedBy());
                })
                .verifyComplete();

        verify(maps).save(any(ValidationMap.class));
    }

    private void activeSubtype() {
        when(subtypes.existsByCode("S")).thenReturn(Mono.just(true));
        when(subtypes.existsByCodeAndBinEfectivo("S", "B")).thenReturn(Mono.just(true));
    }

    private Validation activeValidation(ValidationDataType type) {
        return Validation.rehydrate(1L, "VAL", "desc", type, "A",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().minusDays(2), OffsetDateTime.now().minusDays(1), "user");
    }
}


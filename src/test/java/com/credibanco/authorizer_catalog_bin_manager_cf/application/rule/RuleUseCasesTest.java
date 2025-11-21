package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuleUseCasesTest {

    private ValidationRepository validationRepo;
    private ValidationMapRepository mapRepo;
    private SubtypeReadOnlyRepository subtypeRepo;
    private TransactionalOperator tx;

    @BeforeEach
    void setup() {
        validationRepo = mock(ValidationRepository.class);
        mapRepo = mock(ValidationMapRepository.class);
        subtypeRepo = mock(SubtypeReadOnlyRepository.class);
        tx = TransactionalOperator.create(new NoOpReactiveTransactionManager());
    }

    private static class NoOpReactiveTransactionManager implements ReactiveTransactionManager {
        @NotNull
        @Override
        public Mono<org.springframework.transaction.ReactiveTransaction> getReactiveTransaction(TransactionDefinition definition) {
            return Mono.just(mock(org.springframework.transaction.ReactiveTransaction.class));
        }

        @NotNull
        @Override
        public Mono<Void> commit(@NotNull org.springframework.transaction.ReactiveTransaction transaction) {
            return Mono.empty();
        }

        @NotNull
        @Override
        public Mono<Void> rollback(@NotNull org.springframework.transaction.ReactiveTransaction transaction) {
            return Mono.empty();
        }
    }

    @Test
    void createValidationChecksExistenceAndPersists() {
        CreateValidationService service = new CreateValidationService(validationRepo, tx);
        Validation created = Validation.createNew("CODE", "DESC", ValidationDataType.BOOL, "actor");

        when(validationRepo.existsByCode("CODE")).thenReturn(Mono.just(false));
        when(validationRepo.save(any(Validation.class))).thenReturn(Mono.just(created));

        StepVerifier.create(service.execute("CODE", "DESC", ValidationDataType.BOOL, "actor"))
                .expectNext(created)
                .verifyComplete();

        verify(validationRepo).existsByCode("CODE");
        verify(validationRepo).save(any(Validation.class));
    }

    @Test
    void createValidationFailsOnDuplicateCode() {
        CreateValidationService service = new CreateValidationService(validationRepo, tx);
        when(validationRepo.existsByCode("CODE")).thenReturn(Mono.just(true));

        StepVerifier.create(service.execute("CODE", "DESC", ValidationDataType.BOOL, null))
                .expectErrorSatisfies(err -> assertEquals(AppError.RULES_VALIDATION_ALREADY_EXISTS,
                        ((AppException) err).getError()))
                .verify();
    }

    @Test
    void updateValidationUpdatesDescription() {
        UpdateValidationService service = new UpdateValidationService(validationRepo);
        Validation current = Validation.createNew("CODE", "DESC", ValidationDataType.TEXT, "creator");
        Validation updated = current.updateBasics("NEW DESC", "upd");

        when(validationRepo.findByCode("CODE")).thenReturn(Mono.just(current));
        when(validationRepo.save(any(Validation.class))).thenReturn(Mono.just(updated));

        StepVerifier.create(service.execute("CODE", "NEW DESC", "upd"))
                .expectNext(updated)
                .verifyComplete();
    }

    @Test
    void updateValidationFailsWhenNotFound() {
        UpdateValidationService service = new UpdateValidationService(validationRepo);
        when(validationRepo.findByCode("CODE")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("CODE", "NEW", null))
                .expectErrorSatisfies(err -> assertEquals(AppError.RULES_VALIDATION_NOT_FOUND,
                        ((AppException) err).getError()))
                .verify();
    }

    @Test
    void changeValidationStatusValidatesNewStatus() {
        ChangeValidationStatusService service = new ChangeValidationStatusService(validationRepo, tx);
        Validation current = Validation.createNew("CODE", "DESC", ValidationDataType.TEXT, "creator");
        Validation changed = current.changeStatus("I", "upd");

        when(validationRepo.findByCode("CODE")).thenReturn(Mono.just(current));
        when(validationRepo.save(any(Validation.class))).thenReturn(Mono.just(changed));

        StepVerifier.create(service.execute("CODE", "I", "upd"))
                .expectNext(changed)
                .verifyComplete();

        StepVerifier.create(service.execute("CODE", "X", "upd"))
                .expectErrorSatisfies(err -> assertEquals(AppError.RULES_VALIDATION_INVALID_DATA,
                        ((AppException) err).getError()))
                .verify();
    }

    @Test
    void mapRuleAttachValidatesSubtypeAndValue() {
        MapRuleService service = new MapRuleService(validationRepo, mapRepo, subtypeRepo, tx);
        Validation validation = Validation.rehydrate(5L, "CODE", "DESC", ValidationDataType.BOOL, "A",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusDays(2), OffsetDateTime.now().minusDays(2), "u");
        ValidationMap mapped = ValidationMap.createNew("ST", "123456", 5L, "SI", null, null, "actor");

        when(subtypeRepo.existsByCode("ST")).thenReturn(Mono.just(true));
        when(subtypeRepo.existsByCodeAndBin("ST", "123456")).thenReturn(Mono.just(true));
        when(validationRepo.findByCode("CODE")).thenReturn(Mono.just(validation));
        when(mapRepo.findByNaturalKey("ST", "123456", 5L)).thenReturn(Mono.empty());
        when(mapRepo.save(any(ValidationMap.class))).thenReturn(Mono.just(mapped));

        StepVerifier.create(service.attach("ST", "123456", "CODE", true, "actor"))
                .expectNext(mapped)
                .verifyComplete();
    }

    @Test
    void mapRuleAttachRejectsInactiveValidation() {
        MapRuleService service = new MapRuleService(validationRepo, mapRepo, subtypeRepo, tx);
        Validation validation = Validation.rehydrate(5L, "CODE", "DESC", ValidationDataType.TEXT, "I",
                OffsetDateTime.now().minusDays(2), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusDays(2), OffsetDateTime.now().minusDays(2), "u");

        when(subtypeRepo.existsByCode("ST")).thenReturn(Mono.just(true));
        when(subtypeRepo.existsByCodeAndBin("ST", "123456")).thenReturn(Mono.just(true));
        when(validationRepo.findByCode("CODE")).thenReturn(Mono.just(validation));

        StepVerifier.create(service.attach("ST", "123456", "CODE", "value", "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.RULES_MAP_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void changeRuleStatusPropagatesErrorsWhenMappingMissing() {
        MapRuleService service = new MapRuleService(validationRepo, mapRepo, subtypeRepo, tx);
        Validation validation = Validation.rehydrate(5L, "CODE", "DESC", ValidationDataType.TEXT, "A",
                OffsetDateTime.now().minusDays(2), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusDays(2), OffsetDateTime.now().minusDays(2), "u");

        when(validationRepo.findByCode("CODE")).thenReturn(Mono.just(validation));
        when(mapRepo.findByNaturalKey("ST", "123456", 5L)).thenReturn(Mono.empty());

        StepVerifier.create(service.changeStatus("ST", "123456", "CODE", "A", "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.RULES_MAP_NOT_FOUND, ((AppException) err).getError()))
                .verify();
    }

    @Nested
    class ListingValidations {
        @Test
        void listValidationsRejectsInvalidPagination() {
            ListValidationsService service = new ListValidationsService(validationRepo);

            StepVerifier.create(service.execute("A", null, -1, 0))
                    .expectErrorSatisfies(err -> assertEquals(AppError.RULES_VALIDATION_INVALID_DATA,
                            ((AppException) err).getError()))
                    .verify();
        }

        @Test
        void listValidationsDelegatesToRepository() {
            ListValidationsService service = new ListValidationsService(validationRepo);
            Validation v1 = Validation.createNew("CODE1", "D1", ValidationDataType.TEXT, "actor");
            Validation v2 = Validation.createNew("CODE2", "D2", ValidationDataType.NUMBER, "actor");
            when(validationRepo.findAll("A", "code", 0, 5)).thenReturn(Flux.just(v1, v2));

            StepVerifier.create(service.execute("A", "code", 0, 5))
                    .expectNext(v1)
                    .expectNext(v2)
                    .verifyComplete();

            verify(validationRepo).findAll("A", "code", 0, 5);
        }
    }

    @Nested
    class GetValidation {
        @Test
        void getValidationReturnsMatch() {
            GetValidationService service = new GetValidationService(validationRepo);
            Validation validation = Validation.createNew("CODE", "DESC", ValidationDataType.TEXT, "actor");
            when(validationRepo.findByCode("CODE")).thenReturn(Mono.just(validation));

            StepVerifier.create(service.execute("CODE"))
                    .expectNext(validation)
                    .verifyComplete();
        }

        @Test
        void getValidationFailsWhenMissing() {
            GetValidationService service = new GetValidationService(validationRepo);
            when(validationRepo.findByCode("CODE")).thenReturn(Mono.empty());

            StepVerifier.create(service.execute("CODE"))
                    .expectErrorSatisfies(err -> assertEquals(AppError.RULES_VALIDATION_NOT_FOUND,
                            ((AppException) err).getError()))
                    .verify();
        }
    }

    @Nested
    class ListRulesForSubtype {
        @Test
        void listRulesRejectsInvalidPagination() {
            ListRulesForSubtypeService service = new ListRulesForSubtypeService(mapRepo);

            StepVerifier.create(service.execute("ST", "123456", null, -1, 0))
                    .expectErrorSatisfies(err -> assertEquals(AppError.RULES_MAP_INVALID_DATA,
                            ((AppException) err).getError()))
                    .verify();
        }

        @Test
        void listRulesTrimsBlankStatus() {
            ListRulesForSubtypeService service = new ListRulesForSubtypeService(mapRepo);
            ValidationMap map = ValidationMap.createNew("ST", "123456", 1L, "SI", null, null, "actor");
            when(mapRepo.findResolved(eq("ST"), eq("123456"), any(), eq(1), eq(10))).thenReturn(Flux.just(map));

            StepVerifier.create(service.execute("ST", "123456", "   ", 1, 10))
                    .expectNext(map)
                    .verifyComplete();

            ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
            verify(mapRepo).findResolved(eq("ST"), eq("123456"), statusCaptor.capture(), eq(1), eq(10));
            assertNull(statusCaptor.getValue());
        }
    }

    @Nested
    class MapRule {
        @Test
        void attachFailsWhenValueMissing() {
            MapRuleService service = new MapRuleService(validationRepo, mapRepo, subtypeRepo, tx);

            StepVerifier.create(service.attach("ST", "123456", "CODE", null, "actor"))
                    .expectErrorSatisfies(err -> assertEquals(AppError.RULES_MAP_INVALID_DATA,
                            ((AppException) err).getError()))
                    .verify();
        }

        @Test
        void attachFailsWhenSubtypeMissing() {
            MapRuleService service = new MapRuleService(validationRepo, mapRepo, subtypeRepo, tx);
            when(subtypeRepo.existsByCode("ST")).thenReturn(Mono.just(false));
            when(subtypeRepo.existsByCodeAndBin(anyString(), anyString())).thenReturn(Mono.just(true));
            when(validationRepo.findByCode("CODE")).thenReturn(Mono.just(Validation.createNew("CODE", "DESC",
                    ValidationDataType.BOOL, "actor")));

            StepVerifier.create(service.attach("ST", "123456", "CODE", true, "actor"))
                    .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_NOT_FOUND,
                            ((AppException) err).getError()))
                    .verify();
        }

        @Test
        void attachFailsWhenBinMissing() {
            MapRuleService service = new MapRuleService(validationRepo, mapRepo, subtypeRepo, tx);
            when(subtypeRepo.existsByCode("ST")).thenReturn(Mono.just(true));
            when(subtypeRepo.existsByCodeAndBin("ST", "123456")).thenReturn(Mono.just(false));
            when(validationRepo.findByCode("CODE")).thenReturn(Mono.just(Validation.createNew("CODE", "DESC",
                    ValidationDataType.BOOL, "actor")));

            StepVerifier.create(service.attach("ST", "123456", "CODE", true, "actor"))
                    .expectErrorSatisfies(err -> assertEquals(AppError.BIN_NOT_FOUND,
                            ((AppException) err).getError()))
                    .verify();
        }

        @Test
        void attachFailsWhenValidationMissing() {
            MapRuleService service = new MapRuleService(validationRepo, mapRepo, subtypeRepo, tx);
            when(subtypeRepo.existsByCode("ST")).thenReturn(Mono.just(true));
            when(subtypeRepo.existsByCodeAndBin("ST", "123456")).thenReturn(Mono.just(true));
            when(validationRepo.findByCode("CODE")).thenReturn(Mono.empty());

            StepVerifier.create(service.attach("ST", "123456", "CODE", true, "actor"))
                    .expectErrorSatisfies(err -> assertEquals(AppError.RULES_VALIDATION_NOT_FOUND,
                            ((AppException) err).getError()))
                    .verify();
        }

        @Test
        void attachFailsWhenValidationNotActiveYet() {
            MapRuleService service = new MapRuleService(validationRepo, mapRepo, subtypeRepo, tx);
            Validation validation = Validation.rehydrate(5L, "CODE", "DESC", ValidationDataType.TEXT, "A",
                    OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(5), OffsetDateTime.now().minusDays(1),
                    OffsetDateTime.now().minusDays(1), "creator");

            when(subtypeRepo.existsByCode("ST")).thenReturn(Mono.just(true));
            when(subtypeRepo.existsByCodeAndBin("ST", "123456")).thenReturn(Mono.just(true));
            when(validationRepo.findByCode("CODE")).thenReturn(Mono.just(validation));

            StepVerifier.create(service.attach("ST", "123456", "CODE", "text", "actor"))
                    .expectErrorSatisfies(err -> assertEquals(AppError.RULES_MAP_INVALID_DATA,
                            ((AppException) err).getError()))
                    .verify();
        }

        @Test
        void attachFailsWhenMappingExists() {
            MapRuleService service = new MapRuleService(validationRepo, mapRepo, subtypeRepo, tx);
            Validation validation = Validation.rehydrate(5L, "CODE", "DESC", ValidationDataType.TEXT, "A",
                    OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(5), OffsetDateTime.now().minusDays(2),
                    OffsetDateTime.now().minusDays(2), "creator");
            ValidationMap existing = ValidationMap.createNew("ST", "123456", 5L, null, null, "text", "actor");

            when(subtypeRepo.existsByCode("ST")).thenReturn(Mono.just(true));
            when(subtypeRepo.existsByCodeAndBin("ST", "123456")).thenReturn(Mono.just(true));
            when(validationRepo.findByCode("CODE")).thenReturn(Mono.just(validation));
            when(mapRepo.findByNaturalKey("ST", "123456", 5L)).thenReturn(Mono.just(existing));

            StepVerifier.create(service.attach("ST", "123456", "CODE", "text", "actor"))
                    .expectErrorSatisfies(err -> assertEquals(AppError.RULES_MAP_ALREADY_EXISTS,
                            ((AppException) err).getError()))
                    .verify();
        }

        @Test
        void attachCoercesNumberAndTextValues() {
            MapRuleService service = new MapRuleService(validationRepo, mapRepo, subtypeRepo, tx);
            Validation numberValidation = Validation.rehydrate(6L, "N", "DESC", ValidationDataType.NUMBER, "A",
                    OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusDays(2),
                    OffsetDateTime.now().minusDays(2), "u");
            Validation textValidation = Validation.rehydrate(7L, "T", "DESC", ValidationDataType.TEXT, "A",
                    OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusDays(2),
                    OffsetDateTime.now().minusDays(2), "u");
            ValidationMap numberMapped = ValidationMap.createNew("ST", "123456", 6L, null, 20.0, null, "actor");
            ValidationMap textMapped = ValidationMap.createNew("ST", "123456", 7L, null, null, "text", "actor");

            when(subtypeRepo.existsByCode(anyString())).thenReturn(Mono.just(true));
            when(subtypeRepo.existsByCodeAndBin(anyString(), anyString())).thenReturn(Mono.just(true));
            when(validationRepo.findByCode("N")).thenReturn(Mono.just(numberValidation));
            when(validationRepo.findByCode("T")).thenReturn(Mono.just(textValidation));
            when(mapRepo.findByNaturalKey(anyString(), anyString(), eq(6L))).thenReturn(Mono.empty());
            when(mapRepo.findByNaturalKey(anyString(), anyString(), eq(7L))).thenReturn(Mono.empty());
            when(mapRepo.save(any(ValidationMap.class))).thenReturn(Mono.just(numberMapped)).thenReturn(Mono.just(textMapped));

            StepVerifier.create(service.attach("ST", "123456", "N", "20", "actor"))
                    .expectNext(numberMapped)
                    .verifyComplete();

            StepVerifier.create(service.attach("ST", "123456", "T", "text", "actor"))
                    .expectNext(textMapped)
                    .verifyComplete();
        }

        @Test
        void attachRejectsInvalidCoercionsAndUnsupportedType() {
            MapRuleService service = new MapRuleService(validationRepo, mapRepo, subtypeRepo, tx);
            Validation boolValidation = Validation.rehydrate(10L, "B", "DESC", ValidationDataType.BOOL, "A",
                    OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusDays(2),
                    OffsetDateTime.now().minusDays(2), "u");
            Validation numberValidation = Validation.rehydrate(11L, "N", "DESC", ValidationDataType.NUMBER, "A",
                    OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusDays(2),
                    OffsetDateTime.now().minusDays(2), "u");
            Validation textValidation = Validation.rehydrate(12L, "T", "DESC", ValidationDataType.TEXT, "A",
                    OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusDays(2),
                    OffsetDateTime.now().minusDays(2), "u");
            ValidationDataType unknownType = mock(ValidationDataType.class);
            Validation unsupportedValidation = Validation.rehydrate(13L, "X", "DESC", unknownType, "A",
                    OffsetDateTime.now().minusDays(1), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusDays(2),
                    OffsetDateTime.now().minusDays(2), "u");

            when(subtypeRepo.existsByCode(anyString())).thenReturn(Mono.just(true));
            when(subtypeRepo.existsByCodeAndBin(anyString(), anyString())).thenReturn(Mono.just(true));
            when(mapRepo.findByNaturalKey(anyString(), anyString(), anyLong())).thenReturn(Mono.empty());

            when(validationRepo.findByCode("B")).thenReturn(Mono.just(boolValidation));
            StepVerifier.create(service.attach("ST", "123456", "B", "maybe", "actor"))
                    .expectErrorSatisfies(err -> assertEquals(AppError.RULES_MAP_INVALID_DATA, ((AppException) err).getError()))
                    .verify();

            when(validationRepo.findByCode("N")).thenReturn(Mono.just(numberValidation));
            StepVerifier.create(service.attach("ST", "123456", "N", "abc", "actor"))
                    .expectErrorSatisfies(err -> assertEquals(AppError.RULES_MAP_INVALID_DATA, ((AppException) err).getError()))
                    .verify();

            when(validationRepo.findByCode("T")).thenReturn(Mono.just(textValidation));
            StepVerifier.create(service.attach("ST", "123456", "T", "   ", "actor"))
                    .expectErrorSatisfies(err -> assertEquals(AppError.RULES_MAP_INVALID_DATA, ((AppException) err).getError()))
                    .verify();

            when(validationRepo.findByCode("X")).thenReturn(Mono.just(unsupportedValidation));
            StepVerifier.create(service.attach("ST", "123456", "X", "value", "actor"))
                    .expectError(AppException.class)
                    .verify();
        }

        @Test
        void changeStatusValidatesNewStatus() {
            MapRuleService service = new MapRuleService(validationRepo, mapRepo, subtypeRepo, tx);

            StepVerifier.create(service.changeStatus("ST", "123456", "CODE", "X", "actor"))
                    .expectErrorSatisfies(err -> assertEquals(AppError.RULES_MAP_INVALID_DATA, ((AppException) err).getError()))
                    .verify();
        }

        @Test
        void changeStatusFailsWhenValidationMissing() {
            MapRuleService service = new MapRuleService(validationRepo, mapRepo, subtypeRepo, tx);
            when(validationRepo.findByCode("CODE")).thenReturn(Mono.empty());

            StepVerifier.create(service.changeStatus("ST", "123456", "CODE", "A", "actor"))
                    .expectErrorSatisfies(err -> assertEquals(AppError.RULES_VALIDATION_NOT_FOUND,
                            ((AppException) err).getError()))
                    .verify();
        }

        @Test
        void changeStatusUpdatesMapping() {
            MapRuleService service = new MapRuleService(validationRepo, mapRepo, subtypeRepo, tx);
            Validation validation = Validation.rehydrate(5L, "CODE", "DESC", ValidationDataType.TEXT, "A",
                    OffsetDateTime.now().minusDays(2), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().minusDays(2),
                    OffsetDateTime.now().minusDays(2), "u");
            ValidationMap current = ValidationMap.createNew("ST", "123456", 5L, null, null, "v", "actor");
            ValidationMap updated = current.changeStatus("I", "upd");

            when(validationRepo.findByCode("CODE")).thenReturn(Mono.just(validation));
            when(mapRepo.findByNaturalKey("ST", "123456", 5L)).thenReturn(Mono.just(current));
            when(mapRepo.save(any(ValidationMap.class))).thenReturn(Mono.just(updated));

            StepVerifier.create(service.changeStatus("ST", "123456", "CODE", "I", "upd"))
                    .expectNextMatches(vm -> "I".equals(vm.status()) && "upd".equals(vm.updatedBy()))
                    .verifyComplete();
        }
    }
}

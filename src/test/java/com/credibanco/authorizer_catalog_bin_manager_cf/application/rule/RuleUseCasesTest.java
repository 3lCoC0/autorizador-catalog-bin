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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
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
        @Override
        public Mono<org.springframework.transaction.ReactiveTransaction> getReactiveTransaction(TransactionDefinition definition) {
            return Mono.just(mock(org.springframework.transaction.ReactiveTransaction.class));
        }

        @Override
        public Mono<Void> commit(org.springframework.transaction.ReactiveTransaction transaction) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> rollback(org.springframework.transaction.ReactiveTransaction transaction) {
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
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpdatePlanServiceTest {

    private CommercePlanRepository repository;
    private UpdatePlanService service;

    @BeforeEach
    void setUp() {
        repository = mock(CommercePlanRepository.class);
        service = new UpdatePlanService(repository);
    }

    @Test
    void whenPlanMissingThenEmitNotFound() {
        when(repository.findByCode("PLAN")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("PLAN", "New", null, null, "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.PLAN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(repository).findByCode("PLAN");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenValidationModeInvalidThenWrapInAppException() {
        CommercePlan current = CommercePlan.rehydrate(1L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        when(repository.findByCode("PLAN")).thenReturn(Mono.just(current));

        StepVerifier.create(service.execute("PLAN", "New", null, "OTHER", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.PLAN_INVALID_DATA, appException.getError());
                    assertTrue(appException.getMessage().contains("validationMode"));
                })
                .verify();

        verify(repository).findByCode("PLAN");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenUpdateBasicsFailsThenWrapInAppException() {
        CommercePlan current = CommercePlan.rehydrate(1L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        when(repository.findByCode("PLAN")).thenReturn(Mono.just(current));

        StepVerifier.create(service.execute("PLAN", " ", null, null, "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.PLAN_INVALID_DATA, appException.getError());
                    assertTrue(appException.getMessage().contains("name"));
                })
                .verify();

        verify(repository).findByCode("PLAN");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenRequestValidThenPersistUpdatedPlan() {
        CommercePlan current = CommercePlan.rehydrate(1L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        when(repository.findByCode("PLAN")).thenReturn(Mono.just(current));
        when(repository.save(any(CommercePlan.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute("PLAN", "Updated", "Nueva desc", "merchant_id", "editor"))
                .assertNext(updated -> {
                    assertEquals("PLAN", updated.code());
                    assertEquals("Updated", updated.name());
                    assertEquals(CommerceValidationMode.MERCHANT_ID, updated.validationMode());
                    assertEquals("editor", updated.updatedBy());
                })
                .verifyComplete();

        verify(repository).findByCode("PLAN");
        verify(repository).save(any(CommercePlan.class));
        verifyNoMoreInteractions(repository);
    }
}

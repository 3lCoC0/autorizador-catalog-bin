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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChangePlanStatusServiceTest {

    private CommercePlanRepository repository;
    private ChangePlanStatusService service;

    @BeforeEach
    void setUp() {
        repository = mock(CommercePlanRepository.class);
        service = new ChangePlanStatusService(repository);
    }

    @Test
    void whenStatusInvalidThenEmitError() {
        StepVerifier.create(service.execute("PLAN", "X", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.PLAN_INVALID_DATA, appException.getError());
                    assertTrue(appException.getMessage().contains("status"));
                })
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void whenPlanMissingThenEmitNotFound() {
        when(repository.findByCode("PLAN")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("PLAN", "A", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.PLAN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(repository).findByCode("PLAN");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenDomainRejectsStatusThenWrapInAppException() {
        CommercePlan current = spy(CommercePlan.rehydrate(1L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester"));
        when(repository.findByCode("PLAN")).thenReturn(Mono.just(current));
        doThrow(new IllegalArgumentException("boom")).when(current).changeStatus("A", "tester");

        StepVerifier.create(service.execute("PLAN", "A", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.PLAN_INVALID_DATA, appException.getError());
                    assertEquals("boom", appException.getMessage());
                })
                .verify();

        verify(repository).findByCode("PLAN");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenRequestValidThenPersistNewStatus() {
        CommercePlan current = CommercePlan.rehydrate(1L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        when(repository.findByCode("PLAN")).thenReturn(Mono.just(current));
        when(repository.save(any(CommercePlan.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute("PLAN", "I", "editor"))
                .assertNext(updated -> {
                    assertEquals("PLAN", updated.code());
                    assertEquals("I", updated.status());
                    assertEquals("editor", updated.updatedBy());
                })
                .verifyComplete();

        verify(repository).findByCode("PLAN");
        verify(repository).save(any(CommercePlan.class));
        verifyNoMoreInteractions(repository);
    }
}

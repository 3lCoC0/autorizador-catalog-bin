package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreatePlanServiceTest {

    private CommercePlanRepository repository;
    private TransactionalOperator txOperator;
    private CreatePlanService service;

    @BeforeEach
    void setUp() {
        repository = mock(CommercePlanRepository.class);
        txOperator = mock(TransactionalOperator.class);
        service = new CreatePlanService(repository, txOperator);

        when(txOperator.transactional(any(Publisher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void whenPlanAlreadyExistsThenEmitConflictError() {
        when(repository.existsByCode("PLAN-1")).thenReturn(Mono.just(true));

        StepVerifier.create(service.execute("PLAN-1", "Plan", CommerceValidationMode.MCC, "desc", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.PLAN_ALREADY_EXISTS, appException.getError());
                })
                .verify();

        verify(repository).existsByCode("PLAN-1");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenPlanDataInvalidThenWrapError() {
        when(repository.existsByCode("PLAN-2")).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute("PLAN-2", " ", CommerceValidationMode.MCC, "desc", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.PLAN_INVALID_DATA, appException.getError());
                    assertTrue(appException.getMessage().contains("name"));
                })
                .verify();

        verify(repository).existsByCode("PLAN-2");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenPlanIsValidThenPersistAndReturn() {
        when(repository.existsByCode("PLAN-3")).thenReturn(Mono.just(false));
        when(repository.save(any(CommercePlan.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute("PLAN-3", "Plan", CommerceValidationMode.MCC, "desc", "tester"))
                .assertNext(plan -> {
                    assertEquals("PLAN-3", plan.code());
                    assertEquals("Plan", plan.name());
                    assertEquals(CommerceValidationMode.MCC, plan.validationMode());
                })
                .verifyComplete();

        verify(repository).existsByCode("PLAN-3");
        verify(repository).save(any(CommercePlan.class));
        verifyNoMoreInteractions(repository);
    }
}

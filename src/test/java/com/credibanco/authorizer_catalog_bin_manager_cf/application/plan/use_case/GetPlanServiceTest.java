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

class GetPlanServiceTest {

    private CommercePlanRepository repository;
    private GetPlanService service;

    @BeforeEach
    void setUp() {
        repository = mock(CommercePlanRepository.class);
        service = new GetPlanService(repository);
    }

    @Test
    void whenPlanExistsThenReturnIt() {
        CommercePlan plan = CommercePlan.rehydrate(1L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        when(repository.findByCode("PLAN")).thenReturn(Mono.just(plan));

        StepVerifier.create(service.execute("PLAN"))
                .expectNext(plan)
                .verifyComplete();

        verify(repository).findByCode("PLAN");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenPlanMissingThenEmitNotFound() {
        when(repository.findByCode("UNKNOWN")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("UNKNOWN"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.PLAN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(repository).findByCode("UNKNOWN");
        verifyNoMoreInteractions(repository);
    }
}

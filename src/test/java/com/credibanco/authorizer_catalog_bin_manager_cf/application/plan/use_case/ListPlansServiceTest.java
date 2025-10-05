package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListPlansServiceTest {

    private CommercePlanRepository repository;
    private ListPlansService service;

    @BeforeEach
    void setUp() {
        repository = mock(CommercePlanRepository.class);
        service = new ListPlansService(repository);
    }

    @Test
    void whenPaginationInvalidThenEmitError() {
        StepVerifier.create(service.execute("A", null, -1, 0))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.PLAN_INVALID_DATA, appException.getError());
                    assertTrue(appException.getMessage().contains("page>=0"));
                })
                .verify();

        verifyNoInteractions(repository);
    }

    @Test
    void whenPaginationValidThenDelegateToRepository() {
        CommercePlan plan = CommercePlan.rehydrate(1L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        when(repository.findAll("A", "", 1, 5)).thenReturn(Flux.just(plan));

        StepVerifier.create(service.execute("A", "", 1, 5))
                .expectNext(plan)
                .verifyComplete();

        verify(repository).findAll("A", "", 1, 5);
        verifyNoMoreInteractions(repository);
    }
}

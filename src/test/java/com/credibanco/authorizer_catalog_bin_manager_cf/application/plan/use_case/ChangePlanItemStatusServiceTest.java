package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChangePlanItemStatusServiceTest {

    private CommercePlanRepository planRepository;
    private CommercePlanItemRepository itemRepository;
    private TransactionalOperator txOperator;
    private ChangePlanItemStatusService service;

    @BeforeEach
    void setUp() {
        planRepository = mock(CommercePlanRepository.class);
        itemRepository = mock(CommercePlanItemRepository.class);
        txOperator = mock(TransactionalOperator.class);
        service = new ChangePlanItemStatusService(planRepository, itemRepository, txOperator);

        when(txOperator.transactional(any(Publisher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void whenStatusInvalidThenEmitError() {
        StepVerifier.create(service.execute("PLAN", "123", "X", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.PLAN_ITEM_INVALID_DATA, appException.getError());
                    assertTrue(appException.getMessage().contains("status"));
                })
                .verify();

        verifyNoInteractions(planRepository, itemRepository);
    }

    @Test
    void whenPlanMissingThenEmitNotFound() {
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("PLAN", "123", "A", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.PLAN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(planRepository).findByCode("PLAN");
        verifyNoInteractions(itemRepository);
    }

    @Test
    void whenItemMissingThenEmitNotFound() {
        CommercePlan plan = CommercePlan.rehydrate(1L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.just(plan));
        when(itemRepository.changeStatus(1L, "123", "A", "tester")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("PLAN", "123", "A", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.PLAN_ITEM_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(planRepository).findByCode("PLAN");
        verify(itemRepository).changeStatus(1L, "123", "A", "tester");
    }

    @Test
    void whenRequestValidThenReturnUpdatedItem() {
        CommercePlan plan = CommercePlan.rehydrate(2L, "PLAN", "Plan", CommerceValidationMode.MERCHANT_ID,
                "desc", "A", null, null, "tester");
        PlanItem updated = PlanItem.rehydrate(5L, 2L, "123456789", OffsetDateTime.now(), OffsetDateTime.now(), "editor", "I");

        when(planRepository.findByCode("PLAN")).thenReturn(Mono.just(plan));
        when(itemRepository.changeStatus(2L, "123456789", "I", "editor"))
                .thenReturn(Mono.just(updated));

        StepVerifier.create(service.execute("PLAN", "123456789", "I", "editor"))
                .expectNext(updated)
                .verifyComplete();

        verify(planRepository).findByCode("PLAN");
        verify(itemRepository).changeStatus(2L, "123456789", "I", "editor");
    }
}

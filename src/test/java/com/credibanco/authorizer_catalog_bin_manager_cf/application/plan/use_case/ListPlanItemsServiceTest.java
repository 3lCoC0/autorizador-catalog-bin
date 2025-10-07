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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListPlanItemsServiceTest {

    private CommercePlanRepository planRepository;
    private CommercePlanItemRepository itemRepository;
    private ListPlanItemsService service;

    @BeforeEach
    void setUp() {
        planRepository = mock(CommercePlanRepository.class);
        itemRepository = mock(CommercePlanItemRepository.class);
        service = new ListPlanItemsService(planRepository, itemRepository);
    }

    @Test
    void whenPaginationInvalidThenEmitError() {
        StepVerifier.create(service.list("PLAN", -1, 0, "A"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.PLAN_ITEM_INVALID_DATA, appException.getError());
                    assertTrue(appException.getMessage().contains("page>=0"));
                })
                .verify();

        verifyNoInteractions(planRepository, itemRepository);
    }

    @Test
    void whenPlanMissingThenEmitNotFound() {
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.empty());

        StepVerifier.create(service.list("PLAN", 0, 10, null))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.PLAN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(planRepository).findByCode("PLAN");
        verifyNoInteractions(itemRepository);
    }

    @Test
    void whenPlanExistsThenReturnItems() {
        CommercePlan plan = CommercePlan.rehydrate(1L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.just(plan));
        PlanItem item = PlanItem.rehydrate(5L, 1L, "1234", OffsetDateTime.now(), OffsetDateTime.now(), "tester", "A");
        when(itemRepository.listItems(1L, "A", 0, 5)).thenReturn(Flux.just(item));

        StepVerifier.create(service.list("PLAN", 0, 5, "A"))
                .expectNext(item)
                .verifyComplete();

        verify(planRepository).findByCode("PLAN");
        verify(itemRepository).listItems(1L, "A", 0, 5);
        verifyNoMoreInteractions(itemRepository);
    }
}

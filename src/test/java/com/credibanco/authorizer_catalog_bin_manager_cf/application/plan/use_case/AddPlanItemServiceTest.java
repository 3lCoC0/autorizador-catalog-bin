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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AddPlanItemServiceTest {

    private CommercePlanRepository planRepository;
    private CommercePlanItemRepository itemRepository;
    private TransactionalOperator txOperator;
    private AddPlanItemService service;

    @BeforeEach
    void setUp() {
        planRepository = mock(CommercePlanRepository.class);
        itemRepository = mock(CommercePlanItemRepository.class);
        txOperator = mock(TransactionalOperator.class);
        service = new AddPlanItemService(planRepository, itemRepository, txOperator);

        when(txOperator.transactional(any(Publisher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void addValueWhenPlanMissingThenEmitNotFound() {
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.empty());

        StepVerifier.create(service.addValue("PLAN", "1234", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.PLAN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(planRepository).findByCode("PLAN");
        verifyNoInteractions(itemRepository);
    }

    @Test
    void addValueWhenValueInvalidForMccThenEmitError() {
        CommercePlan plan = CommercePlan.rehydrate(1L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.just(plan));

        StepVerifier.create(service.addValue("PLAN", "12", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.PLAN_ITEM_INVALID_DATA, appException.getError());
                    assertTrue(appException.getMessage().contains("MCC"));
                })
                .verify();

        verify(planRepository).findByCode("PLAN");
        verifyNoInteractions(itemRepository);
    }

    @Test
    void addValueWhenValueInvalidForMerchantThenEmitError() {
        CommercePlan plan = CommercePlan.rehydrate(2L, "PLAN", "Plan", CommerceValidationMode.MERCHANT_ID,
                "desc", "A", null, null, "tester");
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.just(plan));

        StepVerifier.create(service.addValue("PLAN", "123", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.PLAN_ITEM_INVALID_DATA, appException.getError());
                    assertTrue(appException.getMessage().contains("MERCHANT_ID"));
                })
                .verify();

        verify(planRepository).findByCode("PLAN");
        verifyNoInteractions(itemRepository);
    }

    @Test
    void addValueWhenAlreadyExistsThenEmitConflict() {
        CommercePlan plan = CommercePlan.rehydrate(3L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.just(plan));
        PlanItem existing = PlanItem.rehydrate(10L, 3L, "1234", OffsetDateTime.now(), OffsetDateTime.now(), "tester", "A");
        when(itemRepository.findByValue(3L, "1234")).thenReturn(Mono.just(existing));

        StepVerifier.create(service.addValue("PLAN", "1234", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.PLAN_ITEM_INVALID_DATA, appException.getError());
                    assertTrue(appException.getMessage().contains("Ítem ya existe"));
                })
                .verify();

        verify(planRepository).findByCode("PLAN");
        verify(itemRepository).findByValue(3L, "1234");
        verifyNoMoreInteractions(itemRepository);
    }

    @Test
    void addValueWhenInsertFailsWithDuplicateKeyThenWrapError() {
        CommercePlan plan = CommercePlan.rehydrate(4L, "PLAN", "Plan", CommerceValidationMode.MERCHANT_ID,
                "desc", "A", null, null, "tester");
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.just(plan));
        when(itemRepository.findByValue(4L, "123456789")).thenReturn(Mono.empty());
        when(itemRepository.insertMerchant(4L, "123456789", "tester"))
                .thenReturn(Mono.error(new DuplicateKeyException("dup")));

        StepVerifier.create(service.addValue("PLAN", "123456789", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.PLAN_ITEM_INVALID_DATA, appException.getError());
                    assertTrue(appException.getMessage().contains("Ítem ya existe"));
                })
                .verify();

        verify(planRepository).findByCode("PLAN");
        verify(itemRepository).findByValue(4L, "123456789");
        verify(itemRepository).insertMerchant(4L, "123456789", "tester");
    }

    @Test
    void addValueWhenValidMccThenInsertAndReturn() {
        CommercePlan plan = CommercePlan.rehydrate(5L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.just(plan));
        when(itemRepository.findByValue(5L, "1234")).thenReturn(Mono.empty());
        PlanItem saved = PlanItem.rehydrate(11L, 5L, "1234", OffsetDateTime.now(), OffsetDateTime.now(), "tester", "A");
        when(itemRepository.insertMcc(5L, "1234", "tester")).thenReturn(Mono.just(saved));

        StepVerifier.create(service.addValue("PLAN", "1234", "tester"))
                .expectNext(saved)
                .verifyComplete();

        verify(planRepository).findByCode("PLAN");
        verify(itemRepository).findByValue(5L, "1234");
        verify(itemRepository).insertMcc(5L, "1234", "tester");
    }

    @Test
    void addManyWhenPlanMissingThenEmitNotFound() {
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.empty());

        StepVerifier.create(service.addMany("PLAN", List.of("1234"), "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.PLAN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(planRepository).findByCode("PLAN");
        verifyNoInteractions(itemRepository);
    }

    @Test
    void addManyWhenAllValuesInvalidThenReturnSummary() {
        CommercePlan plan = CommercePlan.rehydrate(6L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.just(plan));

        StepVerifier.create(service.addMany("PLAN", List.of("abc", "", "12"), "tester"))
                .assertNext(result -> {
                    assertEquals("PLAN", result.planCode());
                    assertEquals(3, result.totalReceived());
                    assertEquals(0, result.inserted());
                    assertEquals(0, result.duplicates());
                    assertEquals(3, result.invalid());
                    assertEquals(List.of("abc", "12"), result.invalidValues());
                })
                .verifyComplete();

        verify(planRepository).findByCode("PLAN");
        verifyNoInteractions(itemRepository);
    }

    @Test
    void addManyWhenAllValuesAlreadyExistThenReturnDuplicates() {
        CommercePlan plan = CommercePlan.rehydrate(7L, "PLAN", "Plan", CommerceValidationMode.MERCHANT_ID,
                "desc", "A", null, null, "tester");
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.just(plan));
        when(itemRepository.findExistingValues(eq(7L), anyList()))
                .thenReturn(Flux.fromIterable(List.of("000000001", "000000002")));

        StepVerifier.create(service.addMany("PLAN", List.of("000000001", "000000002"), "tester"))
                .assertNext(result -> {
                    assertEquals(2, result.totalReceived());
                    assertEquals(0, result.inserted());
                    assertEquals(2, result.duplicates());
                    assertEquals(List.of("000000001", "000000002"), result.duplicateValues());
                })
                .verifyComplete();

        verify(planRepository).findByCode("PLAN");
        verify(itemRepository).findExistingValues(eq(7L), anyList());
        verifyNoMoreInteractions(itemRepository);
    }

    @Test
    void addManyWhenMerchantValuesValidThenInsertBatches() {
        CommercePlan plan = CommercePlan.rehydrate(8L, "PLAN", "Plan", CommerceValidationMode.MERCHANT_ID,
                "desc", "A", null, null, "tester");
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.just(plan));
        when(itemRepository.findExistingValues(eq(8L), anyList()))
                .thenReturn(Flux.just("000000003"));
        when(itemRepository.insertMerchantBulk(eq(8L), anyList(), eq("tester")))
                .thenAnswer(invocation -> Mono.just(((List<?>) invocation.getArgument(1)).size()));

        List<String> raw = new ArrayList<>();
        raw.add("000000001");
        raw.add("000000002");
        raw.add("000000003"); // existente

        StepVerifier.create(service.addMany("PLAN", raw, "tester"))
                .assertNext(result -> {
                    assertEquals(3, result.totalReceived());
                    assertEquals(2, result.inserted());
                    assertEquals(1, result.duplicates());
                    assertEquals(List.of("000000003"), result.duplicateValues());
                })
                .verifyComplete();

        verify(planRepository).findByCode("PLAN");
        verify(itemRepository).findExistingValues(eq(8L), anyList());
        verify(itemRepository, times(1)).insertMerchantBulk(eq(8L), anyList(), eq("tester"));
    }

    @Test
    void addManyWhenMccValuesValidThenInsertBatches() {
        CommercePlan plan = CommercePlan.rehydrate(9L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.just(plan));
        when(itemRepository.findExistingValues(eq(9L), anyList()))
                .thenReturn(Flux.empty());
        when(itemRepository.insertMccBulk(eq(9L), anyList(), eq("tester")))
                .thenAnswer(invocation -> Mono.just(((List<?>) invocation.getArgument(1)).size()));

        List<String> raw = List.of("1234", "5678");

        StepVerifier.create(service.addMany("PLAN", raw, "tester"))
                .assertNext(result -> {
                    assertEquals(2, result.totalReceived());
                    assertEquals(2, result.inserted());
                    assertEquals(0, result.duplicates());
                    assertEquals(0, result.invalid());
                })
                .verifyComplete();

        verify(planRepository).findByCode("PLAN");
        verify(itemRepository).findExistingValues(eq(9L), anyList());
        verify(itemRepository, times(1)).insertMccBulk(eq(9L), anyList(), eq("tester"));
    }
}

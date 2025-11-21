package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case.AddPlanItemService;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case.AssignPlanToSubtypeService;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case.ChangePlanItemStatusService;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case.ListPlanItemsService;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case.ListPlansService;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlanItemServicesTest {

    private CommercePlanRepository planRepo;
    private CommercePlanItemRepository itemRepo;
    private SubtypePlanRepository subtypePlanRepo;
    private SubtypeReadOnlyRepository subtypeReadOnlyRepo;
    private TransactionalOperator tx;

    @BeforeEach
    void setUp() {
        planRepo = mock(CommercePlanRepository.class);
        itemRepo = mock(CommercePlanItemRepository.class);
        subtypePlanRepo = mock(SubtypePlanRepository.class);
        subtypeReadOnlyRepo = mock(SubtypeReadOnlyRepository.class);
        tx = mock(TransactionalOperator.class);

        lenient().doReturn(Mono.empty()).when(planRepo).findByCode(anyString());
        lenient().when(planRepo.findAll(any(), any(), anyInt(), anyInt())).thenReturn(Flux.empty());

        PlanItem defaultItem = PlanItem.rehydrate(0L, 0L, "", OffsetDateTime.now(), OffsetDateTime.now(), null, "A");

        lenient().when(itemRepo.insertMcc(anyLong(), anyString(), anyString()))
                .thenReturn(Mono.just(defaultItem));
        lenient().when(itemRepo.insertMerchant(anyLong(), anyString(), anyString()))
                .thenReturn(Mono.just(defaultItem));
        lenient().when(itemRepo.findByValue(anyLong(), anyString()))
                .thenReturn(Mono.empty());
        lenient().when(itemRepo.existsActiveByPlanId(anyLong()))
                .thenReturn(Mono.just(true));
        lenient().when(itemRepo.changeStatus(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());
        lenient().when(itemRepo.listItems(anyLong(), anyString(), anyInt(), anyInt()))
                .thenReturn(Flux.empty());

        lenient().when(tx.transactional(ArgumentMatchers.<Mono<?>>any()))
                .thenAnswer(inv -> inv.getArgument(0));

        lenient().when(tx.transactional(ArgumentMatchers.<Flux<?>>any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void addValueValidatesModeAndDuplicates() {
        CommercePlan merchantPlan = CommercePlan.rehydrate(1L, "P1", "Plan", CommerceValidationMode.MERCHANT_ID, "d", "A",
                OffsetDateTime.now(), OffsetDateTime.now(), "by");
        doReturn(Mono.just(merchantPlan)).when(planRepo).findByCode("P1");
        PlanItem created = PlanItem.rehydrate(1L, 99L, "123456789", OffsetDateTime.now(), OffsetDateTime.now(), "by", "A");
        when(itemRepo.findByValue(merchantPlan.planId(), "123456789")).thenReturn(Mono.empty());
        when(itemRepo.insertMerchant(merchantPlan.planId(), "123456789", "actor")).thenReturn(Mono.just(created));

        AddPlanItemService service = new AddPlanItemService(planRepo, itemRepo, tx);

        StepVerifier.create(service.addValue("P1", "123456789", "actor"))
                .expectNext(created)
                .verifyComplete();

        CommercePlan mccPlan = CommercePlan.rehydrate(2L, "P2", "Plan2", CommerceValidationMode.MCC, "d", "A",
                OffsetDateTime.now(), OffsetDateTime.now(), "by");
        doReturn(Mono.just(mccPlan)).when(planRepo).findByCode("P2");

        AppException invalid = assertThrows(AppException.class, () -> service.addValue("P2", "12", "actor").block());
        assertEquals(AppError.PLAN_ITEM_INVALID_DATA, invalid.getError());

        when(itemRepo.findByValue(mccPlan.planId(), "1234")).thenReturn(Mono.just(created));
        AppException duplicate = assertThrows(AppException.class, () -> service.addValue("P2", "1234", "actor").block());
        assertEquals(AppError.PLAN_ITEM_INVALID_DATA, duplicate.getError());

        when(itemRepo.findByValue(mccPlan.planId(), "5678")).thenReturn(Mono.empty());
        when(itemRepo.insertMcc(mccPlan.planId(), "5678", "actor"))
                .thenReturn(Mono.error(new DuplicateKeyException("dup")));

        AppException duplicateKey = assertThrows(AppException.class,
                () -> service.addValue("P2", "5678", "actor").block());
        assertEquals(AppError.PLAN_ITEM_INVALID_DATA, duplicateKey.getError());
    }

    @Test
    void addManyFiltersInvalidsAndCountsResults() {
        CommercePlan plan = CommercePlan.rehydrate(3L, "PCODE", "Plan", CommerceValidationMode.MERCHANT_ID, "d", "A",
                OffsetDateTime.now(), OffsetDateTime.now(), "by");
        doReturn(Mono.just(plan)).when(planRepo).findByCode("PCODE");
        when(itemRepo.findExistingValues(eq(plan.planId()), any())).thenReturn(Flux.just("123456789"));
        when(itemRepo.insertMerchantBulk(eq(plan.planId()), eq(List.of("987654321")), eq("actor")))
                .thenReturn(Mono.just(1));

        AddPlanItemService service = new AddPlanItemService(planRepo, itemRepo, tx);

        StepVerifier.create(service.addMany("PCODE", Arrays.asList(" 123456789 ", "invalid", null, "", "987654321"), "actor"))
                .expectNextMatches(result -> result.inserted() == 1
                        && result.duplicates() == 1
                        && result.invalid() == 3
                        && result.invalidValues().contains("invalid"))
                .verifyComplete();
    }

    @Test
    void assignPlanToSubtypeValidatesSubtypePlanAndItems() {
        AssignPlanToSubtypeService service = new AssignPlanToSubtypeService(planRepo, subtypePlanRepo, subtypeReadOnlyRepo, itemRepo, tx);

        when(subtypeReadOnlyRepo.existsByCode("SUB1")).thenReturn(Mono.just(false));

        StepVerifier.create(service.assign("SUB1", "PLAN", "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) err).getError()))
                .verify();

        when(subtypeReadOnlyRepo.existsByCode("SUB1")).thenReturn(Mono.just(true));
        doReturn(Mono.empty()).when(planRepo).findByCode("PLAN");

        StepVerifier.create(service.assign("SUB1", "PLAN", "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.PLAN_NOT_FOUND, ((AppException) err).getError()))
                .verify();

        CommercePlan plan = CommercePlan.rehydrate(4L, "PLAN", "Name", CommerceValidationMode.MCC, "d", "A",
                OffsetDateTime.now(), OffsetDateTime.now(), "by");
        doReturn(Mono.just(plan)).when(planRepo).findByCode("PLAN");
        when(itemRepo.existsActiveByPlanId(plan.planId())).thenReturn(Mono.just(false));

        StepVerifier.create(service.assign("SUB1", "PLAN", "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.PLAN_ASSIGNMENT_CONFLICT, ((AppException) err).getError()))
                .verify();

        when(itemRepo.existsActiveByPlanId(plan.planId())).thenReturn(Mono.just(true));
        SubtypePlanLink link = SubtypePlanLink.rehydrate("SUB1", 5L, OffsetDateTime.now(), OffsetDateTime.now(), "actor");
        when(subtypePlanRepo.upsert("SUB1", plan.planId(), "actor")).thenReturn(Mono.just(1));
        when(subtypePlanRepo.findBySubtype("SUB1")).thenReturn(Mono.just(link));

        StepVerifier.create(service.assign("SUB1", "PLAN", "actor"))
                .expectNext(link)
                .verifyComplete();

        verify(subtypePlanRepo).upsert("SUB1", plan.planId(), "actor");
    }

    @Test
    void changePlanItemStatusHandlesValidationAndMissingResources() {
        ChangePlanItemStatusService service = new ChangePlanItemStatusService(planRepo, itemRepo, tx);

        StepVerifier.create(service.execute("PLAN", "value", "X", "by"))
                .expectErrorSatisfies(err -> assertEquals(AppError.PLAN_ITEM_INVALID_DATA, ((AppException) err).getError()))
                .verify();

        when(planRepo.findByCode("PLAN")).thenReturn(Mono.empty());
        StepVerifier.create(service.execute("PLAN", "value", "A", "by"))
                .expectErrorSatisfies(err -> assertEquals(AppError.PLAN_NOT_FOUND, ((AppException) err).getError()))
                .verify();

        CommercePlan plan = CommercePlan.rehydrate(5L, "PLAN", "Name", CommerceValidationMode.MCC, "d", "A",
                OffsetDateTime.now(), OffsetDateTime.now(), "by");
        doReturn(Mono.just(plan)).when(planRepo).findByCode("PLAN");
        when(itemRepo.changeStatus(plan.planId(), "value", "A", "by")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("PLAN", "value", "A", "by"))
                .expectErrorSatisfies(err -> assertEquals(AppError.PLAN_ITEM_NOT_FOUND, ((AppException) err).getError()))
                .verify();

        PlanItem updated = PlanItem.rehydrate(10L, 9L, "value", OffsetDateTime.now(), OffsetDateTime.now(), "by", "A");
        when(itemRepo.changeStatus(plan.planId(), "value", "I", "by")).thenReturn(Mono.just(updated));

        StepVerifier.create(service.execute("PLAN", "value", "I", "by"))
                .expectNext(updated)
                .verifyComplete();
    }

    @Test
    void listPlanItemsValidatesPagingAndExistingPlan() {
        ListPlanItemsService service = new ListPlanItemsService(planRepo, itemRepo);

        StepVerifier.create(service.list("CODE", -1, 0, "A"))
                .expectErrorSatisfies(err -> assertEquals(AppError.PLAN_ITEM_INVALID_DATA, ((AppException) err).getError()))
                .verify();

        when(planRepo.findByCode("CODE")).thenReturn(Mono.empty());
        StepVerifier.create(service.list("CODE", 0, 1, "A"))
                .expectErrorSatisfies(err -> assertEquals(AppError.PLAN_NOT_FOUND, ((AppException) err).getError()))
                .verify();

        CommercePlan plan = CommercePlan.rehydrate(6L, "CODE", "Name", CommerceValidationMode.MCC, "d", "A",
                OffsetDateTime.now(), OffsetDateTime.now(), "by");
        doReturn(Mono.just(plan)).when(planRepo).findByCode("CODE");
        PlanItem item = PlanItem.rehydrate(1L, 2L, "v", OffsetDateTime.now(), OffsetDateTime.now(), "by", "A");
        when(itemRepo.listItems(plan.planId(), "A", 0, 1)).thenReturn(Flux.just(item));

        StepVerifier.create(service.list("CODE", 0, 1, "A").collectList())
                .expectNext(List.of(item))
                .verifyComplete();
    }

    @Test
    void listPlansValidatesPagingBeforeDelegating() {
        ListPlansService service = new ListPlansService(planRepo);

        StepVerifier.create(service.execute("A", null, -1, 0))
                .expectErrorSatisfies(err -> assertEquals(AppError.PLAN_INVALID_DATA, ((AppException) err).getError()))
                .verify();

        CommercePlan plan = CommercePlan.rehydrate(7L, "CODE", "Name", CommerceValidationMode.MCC, "d", "A",
                OffsetDateTime.now(), OffsetDateTime.now(), "by");
        when(planRepo.findAll("A", null, 0, 1)).thenReturn(Flux.just(plan));

        StepVerifier.create(service.execute("A", null, 0, 1).collectList())
                .expectNext(List.of(plan))
                .verifyComplete();
    }
}

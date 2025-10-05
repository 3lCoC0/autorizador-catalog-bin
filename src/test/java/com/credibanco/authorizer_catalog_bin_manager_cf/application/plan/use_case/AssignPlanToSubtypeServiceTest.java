package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;
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

class AssignPlanToSubtypeServiceTest {

    private CommercePlanRepository planRepository;
    private SubtypePlanRepository subtypePlanRepository;
    private SubtypeReadOnlyRepository subtypeReadOnlyRepository;
    private CommercePlanItemRepository itemRepository;
    private TransactionalOperator txOperator;
    private AssignPlanToSubtypeService service;

    @BeforeEach
    void setUp() {
        planRepository = mock(CommercePlanRepository.class);
        subtypePlanRepository = mock(SubtypePlanRepository.class);
        subtypeReadOnlyRepository = mock(SubtypeReadOnlyRepository.class);
        itemRepository = mock(CommercePlanItemRepository.class);
        txOperator = mock(TransactionalOperator.class);
        service = new AssignPlanToSubtypeService(planRepository, subtypePlanRepository,
                subtypeReadOnlyRepository, itemRepository, txOperator);

        when(txOperator.transactional(any(Publisher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void whenSubtypeMissingThenEmitNotFound() {
        when(subtypeReadOnlyRepository.existsByCode("ST")).thenReturn(Mono.just(false));

        StepVerifier.create(service.assign("ST", "PLAN", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(subtypeReadOnlyRepository).existsByCode("ST");
        verifyNoInteractions(planRepository, subtypePlanRepository, itemRepository);
    }

    @Test
    void whenPlanMissingThenEmitNotFound() {
        when(subtypeReadOnlyRepository.existsByCode("ST")).thenReturn(Mono.just(true));
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.empty());

        StepVerifier.create(service.assign("ST", "PLAN", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.PLAN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(subtypeReadOnlyRepository).existsByCode("ST");
        verify(planRepository).findByCode("PLAN");
        verifyNoInteractions(subtypePlanRepository, itemRepository);
    }

    @Test
    void whenPlanHasNoActiveItemsThenEmitConflict() {
        CommercePlan plan = CommercePlan.rehydrate(1L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        when(subtypeReadOnlyRepository.existsByCode("ST")).thenReturn(Mono.just(true));
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.just(plan));
        when(itemRepository.existsActiveByPlanId(1L)).thenReturn(Mono.just(false));

        StepVerifier.create(service.assign("ST", "PLAN", "tester"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.PLAN_ASSIGNMENT_CONFLICT, appException.getError());
                    assertTrue(appException.getMessage().contains("no tiene ítems activos"));
                })
                .verify();

        verify(subtypeReadOnlyRepository).existsByCode("ST");
        verify(planRepository).findByCode("PLAN");
        verify(itemRepository).existsActiveByPlanId(1L);
        verifyNoInteractions(subtypePlanRepository);
    }

    @Test
    void whenAllValidThenAssignAndReturnLink() {
        CommercePlan plan = CommercePlan.rehydrate(2L, "PLAN", "Plan", CommerceValidationMode.MCC,
                "desc", "A", null, null, "tester");
        SubtypePlanLink link = SubtypePlanLink.rehydrate("ST", 2L, OffsetDateTime.now(), OffsetDateTime.now(), "tester");

        when(subtypeReadOnlyRepository.existsByCode("ST")).thenReturn(Mono.just(true));
        when(planRepository.findByCode("PLAN")).thenReturn(Mono.just(plan));
        when(itemRepository.existsActiveByPlanId(2L)).thenReturn(Mono.just(true));
        when(subtypePlanRepository.upsert("ST", 2L, "tester")).thenReturn(Mono.just(1));
        when(subtypePlanRepository.findBySubtype("ST")).thenReturn(Mono.just(link));

        StepVerifier.create(service.assign("ST", "PLAN", "tester"))
                .expectNext(link)
                .verifyComplete();

        verify(subtypeReadOnlyRepository).existsByCode("ST");
        verify(planRepository).findByCode("PLAN");
        verify(itemRepository).existsActiveByPlanId(2L);
        verify(subtypePlanRepository).upsert("ST", 2L, "tester");
        verify(subtypePlanRepository).findBySubtype("ST");
    }
}

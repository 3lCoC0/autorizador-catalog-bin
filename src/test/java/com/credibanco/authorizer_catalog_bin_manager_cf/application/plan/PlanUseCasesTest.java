package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case.ChangePlanStatusService;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case.CreatePlanService;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case.GetPlanService;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case.UpdatePlanService;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlanUseCasesTest {

    private CommercePlanRepository repo;
    private TransactionalOperator tx;

    @BeforeEach
    void setUp() {
        repo = mock(CommercePlanRepository.class);
        tx = mock(TransactionalOperator.class);
        when(tx.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tx.transactional(any(Flux.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createPlanRejectsDuplicatesAndMapsValidationErrors() {
        when(repo.existsByCode("DUP")).thenReturn(Mono.just(true));

        CreatePlanService service = new CreatePlanService(repo, tx);

        StepVerifier.create(service.execute("DUP", "NAME", CommerceValidationMode.MERCHANT_ID, "desc", "creator"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.PLAN_ALREADY_EXISTS, ((AppException) err).getError());
                })
                .verify();

        when(repo.existsByCode("BAD")).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute("BAD", "", CommerceValidationMode.MERCHANT_ID, "desc", "creator"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.PLAN_INVALID_DATA, ((AppException) err).getError());
                })
                .verify();
    }

    @Test
    void updatePlanValidatesExistingAggregateAndMode() {
        CommercePlan existing = CommercePlan.createNew("CODE", "NAME", CommerceValidationMode.MERCHANT_ID, "desc", "creator");
        when(repo.findByCode("CODE")).thenReturn(Mono.just(existing));
        when(repo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        UpdatePlanService service = new UpdatePlanService(repo);

        StepVerifier.create(service.execute("CODE", "UPDATED", "new desc", CommerceValidationMode.MCC.name(), "editor"))
                .expectNextMatches(saved -> saved.name().equals("UPDATED") && saved.validationMode() == CommerceValidationMode.MCC)
                .verifyComplete();

        when(repo.findByCode("MISSING")).thenReturn(Mono.empty());
        StepVerifier.create(service.execute("MISSING", "UPDATED", "desc", null, "editor"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.PLAN_NOT_FOUND, ((AppException) err).getError());
                })
                .verify();

        when(repo.findByCode("CODE")).thenReturn(Mono.just(existing));
        StepVerifier.create(service.execute("CODE", "UPDATED", "desc", "INVALID", "editor"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.PLAN_INVALID_DATA, ((AppException) err).getError());
                })
                .verify();
    }

    @Test
    void changeStatusValidatesStatusAndPersists() {
        CommercePlan current = CommercePlan.rehydrate(10L, "CODE", "NAME", CommerceValidationMode.MERCHANT_ID,
                "desc", "A", OffsetDateTime.now().minusDays(1), OffsetDateTime.now().minusDays(1), "creator");
        when(repo.findByCode("CODE")).thenReturn(Mono.just(current));
        when(repo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        ChangePlanStatusService service = new ChangePlanStatusService(repo);

        StepVerifier.create(service.execute("CODE", "I", "actor"))
                .assertNext(updated -> assertEquals("I", updated.status()))
                .verifyComplete();

        ArgumentCaptor<CommercePlan> saved = ArgumentCaptor.forClass(CommercePlan.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().updatedBy()).isEqualTo("actor");

        StepVerifier.create(service.execute("CODE", "X", "actor"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.PLAN_INVALID_DATA, ((AppException) err).getError());
                })
                .verify();

        when(repo.findByCode("MISSING")).thenReturn(Mono.empty());
        StepVerifier.create(service.execute("MISSING", "I", "actor"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.PLAN_NOT_FOUND, ((AppException) err).getError());
                })
                .verify();
    }

    @Test
    void getPlanReturnsAggregateOrNotFound() {
        CommercePlan plan = CommercePlan.createNew("CODE", "NAME", CommerceValidationMode.MERCHANT_ID, "desc", "by");
        when(repo.findByCode("CODE")).thenReturn(Mono.just(plan));
        GetPlanService service = new GetPlanService(repo);

        StepVerifier.create(service.execute("CODE"))
                .expectNext(plan)
                .verifyComplete();

        when(repo.findByCode("MISS")).thenReturn(Mono.empty());
        StepVerifier.create(service.execute("MISS"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.PLAN_NOT_FOUND, ((AppException) err).getError());
                })
                .verify();
    }
}

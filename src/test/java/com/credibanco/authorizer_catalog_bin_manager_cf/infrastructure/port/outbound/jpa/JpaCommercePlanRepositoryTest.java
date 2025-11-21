package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.CommercePlanEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.CommercePlanJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.CommercePlanJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.Mockito.*;

class JpaCommercePlanRepositoryTest {

    private CommercePlanJpaRepository springRepo;
    private JpaCommercePlanRepository repo;

    @BeforeEach
    void setup() {
        springRepo = mock(CommercePlanJpaRepository.class);
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);

        when(tm.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());

        repo = new JpaCommercePlanRepository(springRepo, tm);
    }

    @Test
    void existsAndFindDelegateToJpaRepository() {
        when(springRepo.existsByPlanCode("CODE")).thenReturn(true);
        StepVerifier.create(repo.existsByCode("CODE"))
                .expectNext(true)
                .verifyComplete();

        CommercePlanEntity entity = new CommercePlanEntity();
        entity.setPlanId(5L);
        entity.setPlanCode("CODE");
        entity.setPlanName("NAME");
        entity.setValidationMode("MERCHANT_ID");
        entity.setStatus("A");
        when(springRepo.findByPlanCode("CODE")).thenReturn(Optional.of(entity));

        StepVerifier.create(repo.findByCode("CODE"))
                .expectNextMatches(plan -> plan.planId().equals(5L) && plan.code().equals("CODE"))
                .verifyComplete();
    }

    @Test
    void existsAndFindHandleLengthError() {
        DataAccessException lengthError = new org.springframework.dao.DataIntegrityViolationException(
                "ORA-00910: invalid length", new RuntimeException("ORA-00910"));
        when(springRepo.existsByPlanCode("TOO_LONG")).thenThrow(lengthError);
        when(springRepo.findByPlanCode("TOO_LONG")).thenThrow(lengthError);

        StepVerifier.create(repo.existsByCode("TOO_LONG"))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(repo.findByCode("TOO_LONG"))
                .verifyComplete();
    }

    @Test
    void existsAndFindPropagateUnexpectedErrors() {
        RuntimeException failure = new RuntimeException("boom");
        when(springRepo.existsByPlanCode("BAD")).thenThrow(failure);
        when(springRepo.findByPlanCode("BAD")).thenThrow(failure);

        StepVerifier.create(repo.existsByCode("BAD"))
                .expectErrorMatches(failure::equals)
                .verify();

        StepVerifier.create(repo.findByCode("BAD"))
                .expectErrorMatches(failure::equals)
                .verify();
    }

    @Test
    void saveMapsDomainToEntityAndBack() {
        CommercePlan aggregate = CommercePlan.createNew("CODE", "NAME", CommerceValidationMode.MERCHANT_ID, "desc", "creator");
        when(springRepo.findByPlanCode("CODE")).thenReturn(Optional.empty());
        when(springRepo.save(any(CommercePlanEntity.class))).thenAnswer(inv -> {
            CommercePlanEntity entity = inv.getArgument(0);
            entity.setPlanId(99L);
            return entity;
        });

        StepVerifier.create(repo.save(aggregate))
                .assertNext(saved -> {
                    assertEquals(99L, saved.planId());
                    assertThat(saved.createdAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void saveUpdatesExistingMetadata() {
        CommercePlan aggregate = CommercePlan.rehydrate(null, "CODE", "NAME", CommerceValidationMode.MERCHANT_ID,
                "desc", "A", null, null, "creator");
        CommercePlanEntity existing = CommercePlanJpaMapper.toEntity(aggregate);
        existing.setPlanId(77L);
        OffsetDateTime created = OffsetDateTime.now().minusDays(2);
        existing.setCreatedAt(created);
        existing.setUpdatedAt(created);
        when(springRepo.findByPlanCode("CODE")).thenReturn(Optional.of(existing));
        when(springRepo.save(any(CommercePlanEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(repo.save(aggregate))
                .assertNext(saved -> assertEquals(created, saved.createdAt()))
                .verifyComplete();
    }

    @Test
    void findAllBuildsSpecification() {
        CommercePlan plan = CommercePlan.createNew(
                "CODE", "NAME", CommerceValidationMode.MERCHANT_ID, "desc", "creator"
        );

        when(springRepo.findAll(
                ArgumentMatchers.<Specification<CommercePlanEntity>>any(),
                eq(PageRequest.of(0, 5, Sort.by(Sort.Order.asc("planCode"))))
        )).thenReturn(new PageImpl<>(List.of(CommercePlanJpaMapper.toEntity(plan))));

        StepVerifier.create(repo.findAll("A", "CODE", 0, 5))
                .expectNextMatches(found -> found.code().equals("CODE"))
                .verifyComplete();
    }
}

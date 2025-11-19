package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.CommercePlanEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.CommercePlanJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.CommercePlanJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JpaCommercePlanRepositoryTest {

    private CommercePlanJpaRepository springRepo;
    private PlatformTransactionManager tm;
    private JpaCommercePlanRepository repo;

    @BeforeEach
    void setup() {
        springRepo = mock(CommercePlanJpaRepository.class);
        tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new DefaultTransactionStatus(null, false, false, false, false, null));
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
    void findAllBuildsSpecification() {
        CommercePlan plan = CommercePlan.createNew("CODE", "NAME", CommerceValidationMode.MERCHANT_ID, "desc", "creator");
        when(springRepo.findAll(any(Specification.class), eq(PageRequest.of(0, 5, Sort.by(Sort.Order.asc("planCode"))))))
                .thenReturn(new PageImpl<>(List.of(CommercePlanJpaMapper.toEntity(plan))));

        StepVerifier.create(repo.findAll("A", "CODE", 0, 5))
                .expectNextMatches(found -> found.code().equals("CODE"))
                .verifyComplete();
    }
}

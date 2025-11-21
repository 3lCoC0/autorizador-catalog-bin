package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.CommercePlanItemEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.CommercePlanItemJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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
import java.util.NoSuchElementException;
import static org.mockito.Mockito.*;

class JpaCommercePlanItemRepositoryTest {

    private CommercePlanItemJpaRepository springRepository;
    private JpaCommercePlanItemRepository repo;

    @BeforeEach
    void setup() {
        springRepository = mock(CommercePlanItemJpaRepository.class);
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);

        when(tm.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());  // ← FIX

        repo = new JpaCommercePlanItemRepository(springRepository, tm);
    }

    @Test
    void insertMccAndMerchantPersistEntities() {
        when(springRepository.save(any(CommercePlanItemEntity.class))).thenAnswer(inv -> {
            CommercePlanItemEntity entity = inv.getArgument(0);
            entity.setPlanItemId(10L);
            return entity;
        });

        StepVerifier.create(repo.insertMcc(9L, "1234", "user"))
                .expectNextMatches(item -> item.planId().equals(9L) && item.value().equals("1234"))
                .verifyComplete();

        StepVerifier.create(repo.insertMerchant(9L, "MID", "user"))
                .expectNextMatches(item -> item.value().equals("MID"))
                .verifyComplete();
    }

    @Test
    void changeStatusUpdatesExistingItem() {
        CommercePlanItemEntity entity = new CommercePlanItemEntity();
        entity.setPlanItemId(1L);
        entity.setPlanId(2L);
        entity.setMcc("1234");
        entity.setStatus("A");
        entity.setUpdatedAt(OffsetDateTime.now().minusDays(1));

        when(springRepository.findByPlanIdAndValue(2L, "1234", PageRequest.of(0, 1,
                Sort.by(Sort.Order.desc("planItemId"))))).thenReturn(List.of(entity));
        when(springRepository.save(any(CommercePlanItemEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(repo.changeStatus(2L, "1234", "I", "me"))
                .expectNextMatches(updated -> updated.status().equals("I") && updated.updatedBy().equals("me"))
                .verifyComplete();
    }

    @Test
    void changeStatusReturnsEmptyWhenMissing() {
        when(springRepository.findByPlanIdAndValue(1L, "M", PageRequest.of(0, 1,
                Sort.by(Sort.Order.desc("planItemId"))))).thenReturn(List.of());

        StepVerifier.create(repo.changeStatus(1L, "M", "A", "me"))
                .verifyComplete();
    }

    @Test
    void listItemsBuildsSpecification() {
        CommercePlanItemEntity entity = new CommercePlanItemEntity();
        entity.setPlanItemId(1L);
        entity.setPlanId(3L);
        entity.setMcc("1111");
        entity.setStatus("A");

        when(springRepository.findAll(
                ArgumentMatchers.<Specification<CommercePlanItemEntity>>any(),
                eq(PageRequest.of(0, 2, Sort.by(Sort.Order.asc("mcc"), Sort.Order.asc("merchantId"))))
        )).thenReturn(new PageImpl<>(List.of(entity)));

        StepVerifier.create(repo.listItems(3L, null, 0, 2))
                .expectNextMatches(item -> item.value().equals("1111"))
                .verifyComplete();
    }

    @Test
    void findByValueAndExistsActiveDelegated() {
        CommercePlanItemEntity entity = new CommercePlanItemEntity();
        entity.setPlanItemId(5L);
        entity.setPlanId(4L);
        entity.setMerchantId("MID");
        entity.setStatus("A");
        when(springRepository.findByPlanIdAndValue(4L, "MID", PageRequest.of(0, 1,
                Sort.by(Sort.Order.desc("planItemId"))))).thenReturn(List.of(entity));

        StepVerifier.create(repo.findByValue(4L, "MID"))
                .expectNextMatches(found -> found.value().equals("MID") && found.planId().equals(4L))
                .verifyComplete();

        when(springRepository.existsByPlanIdAndStatus(4L, "A")).thenReturn(true);
        StepVerifier.create(repo.existsActiveByPlanId(4L))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void findByValueThrowsWhenMissing() {
        when(springRepository.findByPlanIdAndValue(7L, "MISSING", PageRequest.of(0, 1,
                Sort.by(Sort.Order.desc("planItemId"))))).thenReturn(List.of());

        StepVerifier.create(repo.findByValue(7L, "MISSING"))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    void findExistingValuesShortCircuitsOnEmptyInput() {
        StepVerifier.create(repo.findExistingValues(1L, List.of()))
                .verifyComplete();
    }

    @Test
    void bulkInsertionSkipsDuplicates() {
        when(springRepository.findExistingValues(9L, List.of("1", "2"))).thenReturn(List.of("2"));
        when(springRepository.save(any(CommercePlanItemEntity.class))).thenReturn(new CommercePlanItemEntity());

        StepVerifier.create(repo.insertMccBulk(9L, List.of("1", "2"), "me"))
                .expectNext(1)
                .verifyComplete();

        StepVerifier.create(repo.insertMerchantBulk(9L, List.of("3", "3", "4"), "me"))
                .expectNext(2)
                .verifyComplete();
    }

    @Test
    void bulkInsertionReturnsZeroWhenEmpty() {
        StepVerifier.create(repo.insertMccBulk(9L, List.of(), "me"))
                .expectNext(0)
                .verifyComplete();

        StepVerifier.create(repo.insertMerchantBulk(9L, null, "me"))
                .expectNext(0)
                .verifyComplete();
    }
}

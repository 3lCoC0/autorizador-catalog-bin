package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeCommercePlanEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.SubtypePlanJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.SubtypeCommercePlanJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JpaSubtypePlanRepositoryTest {

    private SubtypeCommercePlanJpaRepository springRepository;
    private PlatformTransactionManager tm;
    private JpaSubtypePlanRepository repo;

    @BeforeEach
    void setup() {
        springRepository = mock(SubtypeCommercePlanJpaRepository.class);
        tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new DefaultTransactionStatus(null, false, false, false, false, null));
        repo = new JpaSubtypePlanRepository(springRepository, tm);
    }

    @Test
    void upsertCreatesNewWhenMissing() {
        when(springRepository.findBySubtypeCode("S1")).thenReturn(Optional.empty());
        when(springRepository.save(any(SubtypeCommercePlanEntity.class))).thenAnswer(inv -> {
            SubtypeCommercePlanEntity entity = inv.getArgument(0);
            entity.setSubtypePlanId(1L);
            return entity;
        });

        StepVerifier.create(repo.upsert("S1", 9L, "user"))
                .expectNext(1)
                .verifyComplete();
    }

    @Test
    void upsertUpdatesExisting() {
        SubtypeCommercePlanEntity existing = new SubtypeCommercePlanEntity();
        existing.setSubtypePlanId(5L);
        existing.setSubtypeCode("S1");
        existing.setPlanId(4L);
        existing.setCreatedAt(OffsetDateTime.now().minusDays(2));
        when(springRepository.findBySubtypeCode("S1")).thenReturn(Optional.of(existing));
        when(springRepository.save(any(SubtypeCommercePlanEntity.class))).thenReturn(existing);

        StepVerifier.create(repo.upsert("S1", 10L, "me"))
                .expectNext(1)
                .verifyComplete();
    }

    @Test
    void findBySubtypeReturnsOrThrows() {
        SubtypeCommercePlanEntity entity = new SubtypeCommercePlanEntity();
        entity.setSubtypePlanId(1L);
        entity.setSubtypeCode("S1");
        entity.setPlanId(2L);
        when(springRepository.findBySubtypeCode("S1")).thenReturn(Optional.of(entity));

        StepVerifier.create(repo.findBySubtype("S1"))
                .expectNextMatches(link -> link.subtypeCode().equals("S1") && link.planId().equals(2L))
                .verifyComplete();

        when(springRepository.findBySubtypeCode("NONE")).thenReturn(Optional.empty());
        StepVerifier.create(repo.findBySubtype("NONE"))
                .expectErrorMessage("SUBTYPE plan not found: NONE")
                .verify();
    }
}

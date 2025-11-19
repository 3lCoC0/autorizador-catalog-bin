package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeEntityId;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.SubtypeJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.SubtypeJpaRepository;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JpaSubtypeRepositoryTest {

    private SubtypeJpaRepository springRepo;
    private PlatformTransactionManager tm;
    private JpaSubtypeRepository repo;

    @BeforeEach
    void setup() {
        springRepo = mock(SubtypeJpaRepository.class);
        tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new DefaultTransactionStatus(null, false, false, false, false, null));
        repo = new JpaSubtypeRepository(springRepo, tm);
    }

    @Test
    void existsChecksDelegateMethods() {
        when(springRepo.existsById(new SubtypeEntityId("ABC", "123456"))).thenReturn(true);
        StepVerifier.create(repo.existsByPk("123456", "ABC"))
                .expectNext(true)
                .verifyComplete();

        when(springRepo.existsByIdBinAndBinExt("123456", "07")).thenReturn(true);
        StepVerifier.create(repo.existsByBinAndExt("123456", "07"))
                .expectNext(true)
                .verifyComplete();

        when(springRepo.existsByIdSubtypeCode("ABC")).thenReturn(true);
        StepVerifier.create(repo.existsBySubtypeCode("ABC"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void savePersistsAggregateAndMapsBack() {
        Subtype aggregate = Subtype.createNew("ABC", "123456", "NAME", "DESC", "CC", "123", "7", "creator");
        SubtypeEntity entity = SubtypeJpaMapper.toEntity(aggregate);
        when(springRepo.save(any(SubtypeEntity.class))).thenReturn(entity);

        StepVerifier.create(repo.save(aggregate))
                .expectNextMatches(saved -> saved.bin().equals("123456") && saved.binEfectivo().equals("1234567"))
                .verifyComplete();
    }

    @Test
    void findByPkMapsFromEntity() {
        Subtype aggregate = Subtype.rehydrate("ABC", "123456", "NAME", "DESC", "A",
                "CC", "123", "07", "12345607", 10L, OffsetDateTime.now(), OffsetDateTime.now(), "auditor");
        when(springRepo.findById(new SubtypeEntityId("ABC", "123456")))
                .thenReturn(Optional.of(SubtypeJpaMapper.toEntity(aggregate)));

        StepVerifier.create(repo.findByPk("123456", "ABC"))
                .expectNextMatches(found -> found.subtypeId().equals(10L) && found.binExt().equals("07"))
                .verifyComplete();
    }

    @Test
    void findAllBuildsSpecificationAndSorts() {
        Subtype aggregate = Subtype.createNew("ABC", "123456", "NAME", "DESC", null, null, null, null);
        when(springRepo.findAll(any(Specification.class), eq(PageRequest.of(0, 5, Sort.by(Sort.Order.asc("id.bin"), Sort.Order.asc("id.subtypeCode"))))))
                .thenReturn(new PageImpl<>(List.of(SubtypeJpaMapper.toEntity(aggregate))));

        StepVerifier.create(repo.findAll("123456", null, null, 0, 5))
                .expectNextMatches(s -> s.subtypeCode().equals("ABC"))
                .verifyComplete();
    }
}

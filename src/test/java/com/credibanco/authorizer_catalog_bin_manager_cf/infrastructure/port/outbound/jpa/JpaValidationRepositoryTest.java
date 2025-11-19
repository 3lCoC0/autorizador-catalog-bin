package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.ValidationEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.ValidationJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.ValidationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.List;
import java.util.Optional;

import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JpaValidationRepositoryTest {

    private ValidationJpaRepository springRepository;
    private PlatformTransactionManager tm;
    private JpaValidationRepository repo;

    @BeforeEach
    void setup() {
        springRepository = mock(ValidationJpaRepository.class);
        tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new DefaultTransactionStatus(null, false, false, false, false, null));
        repo = new JpaValidationRepository(springRepository, tm);
    }

    @Test
    void existsByCodeDelegatesToSpringData() {
        when(springRepository.existsByCode("CODE")).thenReturn(true);

        StepVerifier.create(repo.existsByCode("CODE"))
                .expectNext(true)
                .verifyComplete();

        verify(springRepository).existsByCode("CODE");
    }

    @Test
    void saveMapsAndPersistsEntity() {
        Validation aggregate = Validation.createNew("CODE", "DESC", ValidationDataType.TEXT, "actor");
        ValidationEntity entity = ValidationJpaMapper.toEntity(aggregate);
        when(springRepository.findByCode("CODE")).thenReturn(Optional.of(entity));
        when(springRepository.save(any(ValidationEntity.class))).thenReturn(entity);

        StepVerifier.create(repo.save(aggregate))
                .expectNextMatches(saved -> saved.code().equals("CODE"))
                .verifyComplete();

        verify(springRepository).save(any(ValidationEntity.class));
    }

    @Test
    void findByCodeReturnsMappedDomain() {
        Validation aggregate = Validation.createNew("CODE", "DESC", ValidationDataType.TEXT, "actor");
        when(springRepository.findByCode("CODE")).thenReturn(Optional.of(ValidationJpaMapper.toEntity(aggregate)));

        StepVerifier.create(repo.findByCode("CODE"))
                .expectNextMatches(v -> v.code().equals("CODE"))
                .verifyComplete();
    }

    @Test
    void findAllBuildsSpecificationAndSorts() {
        Validation aggregate = Validation.createNew("CODE", "DESC", ValidationDataType.TEXT, "actor");
        when(springRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 5, Sort.by("code").ascending()))))
                .thenReturn(new PageImpl<>(List.of(ValidationJpaMapper.toEntity(aggregate))));

        StepVerifier.create(repo.findAll("A", "cod", 0, 5).collectList())
                .expectNextMatches(list -> list.size() == 1 && list.get(0).code().equals("CODE"))
                .verifyComplete();
    }
}

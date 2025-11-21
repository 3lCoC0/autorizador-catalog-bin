package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeEntityId;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.ValidationEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.ValidationMapEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.ValidationMapJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.SubtypeJpaRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.ValidationJpaRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.ValidationMapJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;


import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.transaction.support.SimpleTransactionStatus;
import reactor.test.StepVerifier;
import static org.mockito.Mockito.*;

class JpaValidationMapRepositoryTest {

    private ValidationMapJpaRepository springRepository;
    private SubtypeJpaRepository subtypeRepository;
    private ValidationJpaRepository validationRepository;
    private JpaValidationMapRepository repo;

    @BeforeEach
    void setup() {
        springRepository = mock(ValidationMapJpaRepository.class);
        subtypeRepository = mock(SubtypeJpaRepository.class);
        validationRepository = mock(ValidationJpaRepository.class);
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        repo = new JpaValidationMapRepository(springRepository, subtypeRepository, validationRepository, tm);
    }

    @Test
    void existsActiveDelegatesToSpringData() {
        when(springRepository.existsBySubtypeCodeAndBinAndValidationIdAndStatus("ST", "123456", 1L, "A"))
                .thenReturn(true);

        StepVerifier.create(repo.existsActive("ST", "123456", 1L))
                .expectNext(true)
                .verifyComplete();

        verify(springRepository).existsBySubtypeCodeAndBinAndValidationIdAndStatus("ST", "123456", 1L, "A");
    }

    @Test
    void savePersistsWhenSubtypeAndValidationAreValid() {
        ValidationMap map = ValidationMap.createNew("ST", "123456", 1L, "SI", null, null, "actor");
        ValidationEntity validationEntity = new ValidationEntity();
        validationEntity.setValidationId(1L);
        validationEntity.setStatus("A");
        validationEntity.setValidFrom(OffsetDateTime.now().minusDays(1));
        validationEntity.setValidTo(OffsetDateTime.now().plusDays(1));

        when(subtypeRepository.existsById(new SubtypeEntityId("ST", "123456"))).thenReturn(true);
        when(validationRepository.findActiveById(eq(1L), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(validationEntity));
        when(springRepository.findBySubtypeCodeAndBinAndValidationId("ST", "123456", 1L))
                .thenReturn(Optional.empty());
        when(springRepository.save(any(ValidationMapEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(repo.save(map))
                .expectNextMatches(saved -> saved.validationId().equals(1L))
                .verifyComplete();

        verify(springRepository).save(any(ValidationMapEntity.class));
    }

    @Test
    void saveFailsWhenSubtypeMissing() {
        ValidationMap map = ValidationMap.createNew("ST", "123456", 1L, null, null, null, "actor");
        when(subtypeRepository.existsById(new SubtypeEntityId("ST", "123456"))).thenReturn(false);

        StepVerifier.create(repo.save(map))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    void saveFailsWhenValidationIsNotActive() {
        ValidationMap map = ValidationMap.createNew("ST", "123456", 1L, null, null, null, "actor");
        when(subtypeRepository.existsById(new SubtypeEntityId("ST", "123456"))).thenReturn(true);
        when(validationRepository.findActiveById(eq(1L), any(OffsetDateTime.class)))
                .thenReturn(Optional.empty());

        StepVerifier.create(repo.save(map))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    void saveUpdatesExistingEntityWithoutOverwritingCreatedAt() {
        ValidationMap map = ValidationMap.createNew("ST", "123456", 1L, "SI", 5.0, "text", "actor");
        OffsetDateTime past = OffsetDateTime.now().minusDays(2);
        ValidationMapEntity existing = new ValidationMapEntity();
        existing.setMapId(10L);
        existing.setSubtypeCode("ST");
        existing.setBin("123456");
        existing.setValidationId(1L);
        existing.setStatus("I");
        existing.setCreatedAt(past);
        existing.setUpdatedAt(past);

        when(subtypeRepository.existsById(new SubtypeEntityId("ST", "123456"))).thenReturn(true);
        when(validationRepository.findActiveById(eq(1L), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(new ValidationEntity()));
        when(springRepository.findBySubtypeCodeAndBinAndValidationId("ST", "123456", 1L))
                .thenReturn(Optional.of(existing));
        when(springRepository.save(any(ValidationMapEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        StepVerifier.create(repo.save(map))
                .expectNextMatches(saved -> saved.createdAt().equals(past) && "SI".equals(saved.valueFlag()))
                .verifyComplete();
    }

    @Test
    void findByNaturalKeyReturnsMono() {
        ValidationMap map = ValidationMap.createNew("ST", "123456", 1L, null, null, null, "actor");
        when(springRepository.findBySubtypeCodeAndBinAndValidationId("ST", "123456", 1L))
                .thenReturn(Optional.of(ValidationMapJpaMapper.toEntity(map)));

        StepVerifier.create(repo.findByNaturalKey("ST", "123456", 1L))
                .expectNextMatches(found -> found.validationId().equals(1L))
                .verifyComplete();
    }

    @Test
    void findByNaturalKeyReturnsEmptyWhenNotFound() {
        when(springRepository.findBySubtypeCodeAndBinAndValidationId("ST", "123456", 1L))
                .thenReturn(Optional.empty());

        StepVerifier.create(repo.findByNaturalKey("ST", "123456", 1L))
                .verifyComplete();
    }

    @Test
    void findAllUsesSpecificationAndPaging() {
        ValidationMap map = ValidationMap.createNew("ST", "123456", 1L, null, null, null, "actor");

        when(springRepository.findAll(
                ArgumentMatchers.<Specification<ValidationMapEntity>>any(),
                eq(PageRequest.of(
                        0,
                        10,
                        Sort.by(
                                Sort.Order.asc("subtypeCode"),
                                Sort.Order.asc("bin"),
                                Sort.Order.asc("validationId")
                        )
                ))
        )).thenReturn(new PageImpl<>(List.of(ValidationMapJpaMapper.toEntity(map))));

        StepVerifier.create(repo.findAll("ST", "123456", "A", 0, 10).collectList())
                .expectNextMatches(list -> list.size() == 1 && list.getFirst().validationId().equals(1L))
                .verifyComplete();
    }

    @Test
    void findResolvedDelegatesToQuery() {
        ValidationMap map = ValidationMap.createNew("ST", "123456", 1L, null, null, null, "actor");
        when(springRepository.findResolved(eq("ST"), eq("123456"), eq("A"), any(PageRequest.class)))
                .thenReturn(List.of(ValidationMapJpaMapper.toEntity(map)));

        StepVerifier.create(repo.findResolved("ST", "123456", "A", 0, 5).collectList())
                .expectNextMatches(list -> list.size() == 1 && list.getFirst().validationId().equals(1L))
                .verifyComplete();
    }
}

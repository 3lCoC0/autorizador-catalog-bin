package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.BinEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.BinJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.BinJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;


import java.util.List;
import java.util.Optional;

import org.springframework.transaction.support.SimpleTransactionStatus;

import reactor.test.StepVerifier;
import static org.mockito.Mockito.*;

class JpaBinRepositoryTest {

    private BinJpaRepository springRepository;
    private JpaBinRepository repo;

    @BeforeEach
    void setup() {
        springRepository = mock(BinJpaRepository.class);
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());
        repo = new JpaBinRepository(springRepository, tm);
    }

    @Test
    void existsByIdDelegatesToSpringData() {
        when(springRepository.existsById("123456")).thenReturn(true);
        StepVerifier.create(repo.existsById("123456"))
                .expectNext(true)
                .verifyComplete();
        verify(springRepository).existsById("123456");
    }

    @Test
    void savePersistsAndMapsEntity() {
        Bin aggregate = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null);
        BinEntity entity = BinJpaMapper.toEntity(aggregate);
        when(springRepository.save(any(BinEntity.class))).thenReturn(entity);
        StepVerifier.create(repo.save(aggregate))
                .expectNextMatches(saved -> saved.bin().equals("123456"))
                .verifyComplete();
        verify(springRepository).save(any(BinEntity.class));
    }

    @Test
    void findByIdReturnsMappedDomain() {
        Bin aggregate = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null);
        when(springRepository.findById("123456")).thenReturn(Optional.of(BinJpaMapper.toEntity(aggregate)));

        StepVerifier.create(repo.findById("123456"))
                .expectNextMatches(found -> found.bin().equals("123456"))
                .verifyComplete();
    }

    @Test
    void findAllMapsPagedResult() {
        Bin aggregate = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null);
        when(springRepository.findAll(PageRequest.of(0, 10, Sort.by("bin").ascending())))
                .thenReturn(new PageImpl<>(List.of(BinJpaMapper.toEntity(aggregate))));

        StepVerifier.create(repo.findAll(0, 10))
                .expectNextMatches(bin -> bin.bin().equals("123456"))
                .verifyComplete();
    }

    @Test
    void getExtConfigReadsProjectedFields() {
        Bin aggregate = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "Y", 1, null);
        when(springRepository.findById("123456")).thenReturn(Optional.of(BinJpaMapper.toEntity(aggregate)));

        StepVerifier.create(repo.getExtConfig("123456"))
                .expectNextMatches(config -> config.usesBinExt().equals("Y") && config.binExtDigits() == 1)
                .verifyComplete();
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.IdTypeEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.IdTypeJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

class JpaIdTypeReadOnlyRepositoryTest {

    private IdTypeJpaRepository repository;
    private JpaIdTypeReadOnlyRepository repo;

    @BeforeEach
    void setup() {
        repository = mock(IdTypeJpaRepository.class);
        repo = new JpaIdTypeReadOnlyRepository(repository);
    }

    @Test
    void existsByIdDelegatesToRepository() {
        when(repository.existsById("CC")).thenReturn(true);

        StepVerifier.create(repo.existsById("CC"))
                .expectNext(true)
                .verifyComplete();

        verify(repository).existsById("CC");
    }

    @Test
    void findAllCodesExtractsIdTypeCodes() {
        IdTypeEntity entity = new IdTypeEntity();
        entity.setIdTypeCode("CC");
        IdTypeEntity entity2 = new IdTypeEntity();
        entity2.setIdTypeCode("PP");
        when(repository.findAll()).thenReturn(List.of(entity, entity2));

        StepVerifier.create(repo.findAllCodes())
                .expectNext(List.of("CC", "PP"))
                .verifyComplete();

        verify(repository).findAll();
    }
}

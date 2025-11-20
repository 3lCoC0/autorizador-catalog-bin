package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.SubtypeJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

class JpaSubtypeReadOnlyRepositoryTest {

    private SubtypeJpaRepository repository;
    private JpaSubtypeReadOnlyRepository repo;

    @BeforeEach
    void setup() {
        repository = mock(SubtypeJpaRepository.class);
        repo = new JpaSubtypeReadOnlyRepository(repository);
    }

    @Test
    void isActiveDelegatesToRepository() {
        when(repository.existsByIdSubtypeCodeAndStatus("SUB", "A")).thenReturn(true);

        StepVerifier.create(repo.isActive("SUB"))
                .expectNext(true)
                .verifyComplete();

        verify(repository).existsByIdSubtypeCodeAndStatus("SUB", "A");
    }

    @Test
    void existsByCodeDelegatesToRepository() {
        when(repository.existsByIdSubtypeCode("SUB")).thenReturn(true);

        StepVerifier.create(repo.existsByCode("SUB"))
                .expectNext(true)
                .verifyComplete();

        verify(repository).existsByIdSubtypeCode("SUB");
    }

    @Test
    void existsByCodeAndBinDelegatesToRepository() {
        when(repository.existsByIdSubtypeCodeAndIdBin("SUB", "123456")).thenReturn(true);

        StepVerifier.create(repo.existsByCodeAndBin("SUB", "123456"))
                .expectNext(true)
                .verifyComplete();

        verify(repository).existsByIdSubtypeCodeAndIdBin("SUB", "123456");
    }
}

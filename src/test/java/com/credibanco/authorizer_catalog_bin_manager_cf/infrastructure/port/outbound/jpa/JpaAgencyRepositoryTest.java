package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.AgencyEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.AgencyEntityId;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.AgencyJpaRepository;
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
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JpaAgencyRepositoryTest {

    private AgencyJpaRepository springRepository;
    private PlatformTransactionManager tm;
    private JpaAgencyRepository repo;

    @BeforeEach
    void setup() {
        springRepository = mock(AgencyJpaRepository.class);
        tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new DefaultTransactionStatus(null, false, false, false, false, null));
        repo = new JpaAgencyRepository(springRepository, tm);
    }

    @Test
    void existsAndFindByPkDelegateToRepository() {
        AgencyEntityId id = new AgencyEntityId("S1", "A1");
        when(springRepository.existsById(id)).thenReturn(true);

        StepVerifier.create(repo.existsByPk("S1", "A1"))
                .expectNext(true)
                .verifyComplete();

        AgencyEntity entity = new AgencyEntity();
        entity.setId(id);
        entity.setName("Agency");
        entity.setStatus("A");
        when(springRepository.findById(id)).thenReturn(java.util.Optional.of(entity));

        StepVerifier.create(repo.findByPk("S1", "A1"))
                .expectNextMatches(found -> found.agencyCode().equals("A1") && found.subtypeCode().equals("S1"))
                .verifyComplete();
    }

    @Test
    void findByPkThrowsWhenMissing() {
        when(springRepository.findById(new AgencyEntityId("S1", "A1")))
                .thenReturn(java.util.Optional.empty());

        StepVerifier.create(repo.findByPk("S1", "A1"))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    void savePersistsNewAndExistingValues() {
        Agency aggregate = newAgency("S1", "A1");
        when(springRepository.save(any(AgencyEntity.class))).thenAnswer(inv -> {
            AgencyEntity entity = inv.getArgument(0);
            entity.setId(new AgencyEntityId("S1", "A1"));
            return entity;
        });

        StepVerifier.create(repo.save(aggregate))
                .expectNextMatches(saved -> saved.createdAt() != null && saved.updatedAt() != null)
                .verifyComplete();

        OffsetDateTime created = OffsetDateTime.now().minusDays(1);
        Agency existing = new Agency("S1", "A1", "Agency", "nit", "addr", "phone", "mun",
                "embHigh", "embPins", "cardCust", "cardCustId", "cardCustS", "cardCustSid",
                "pinCust", "pinCustId", "pinCustS", "pinCustSid", "desc", "A", created, created, "u1");
        AgencyEntity existingEntity = new AgencyEntity();
        existingEntity.setId(new AgencyEntityId("S1", "A1"));
        existingEntity.setName("Agency");
        existingEntity.setStatus("A");
        existingEntity.setCreatedAt(created);
        existingEntity.setUpdatedAt(created);
        when(springRepository.save(any(AgencyEntity.class))).thenReturn(existingEntity);

        StepVerifier.create(repo.save(existing))
                .expectNextMatches(saved -> saved.createdAt().equals(created))
                .verifyComplete();
    }

    @Test
    void findAllBuildsSpecification() {
        AgencyEntity mapped = new AgencyEntity();
        mapped.setId(new AgencyEntityId("S1", "A1"));
        mapped.setName("Agency");
        mapped.setStatus("A");
        when(springRepository.findAll(any(Specification.class),
                eq(PageRequest.of(0, 2, Sort.by(Sort.Order.asc("id.subtypeCode"), Sort.Order.asc("id.agencyCode"))))))
                .thenReturn(new PageImpl<>(List.of(mapped)));

        StepVerifier.create(repo.findAll("S1", "A", "age", 0, 2))
                .expectNextMatches(found -> found.agencyCode().equals("A1"))
                .verifyComplete();
    }

    @Test
    void existenceAggregationsDelegated() {
        when(springRepository.existsByIdSubtypeCodeAndStatusAndIdAgencyCodeNot("S1", "A", "EX"))
                .thenReturn(true);
        when(springRepository.countByIdSubtypeCodeAndStatus("S1", "A"))
                .thenReturn(3L);

        StepVerifier.create(repo.existsAnotherActive("S1", "EX"))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(repo.countActiveBySubtypeCode("S1"))
                .expectNext(3L)
                .verifyComplete();
    }

    private Agency newAgency(String subtype, String agency) {
        return Agency.createNew(subtype, agency, "Agency", "nit", "addr", "phone", "mun",
                "embHigh", "embPins", "cardCust", "cardCustId", "cardCustS", "cardCustSid",
                "pinCust", "pinCustId", "pinCustS", "pinCustSid", "desc", "user");
    }
}

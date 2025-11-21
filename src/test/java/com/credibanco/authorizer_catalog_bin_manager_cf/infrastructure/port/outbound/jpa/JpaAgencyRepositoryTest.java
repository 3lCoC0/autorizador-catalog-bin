package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.AgencyEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.AgencyEntityId;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.AgencyJpaRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.Mockito;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class JpaAgencyRepositoryTest {

    @Test
    void saveSetsAuditFieldsWhenMissing() {
        AgencyJpaRepository repository = mock(AgencyJpaRepository.class);
        JpaAgencyRepository jpaAgencyRepository = new JpaAgencyRepository(repository, new NoOpTransactionManager());

        Agency agency = Agency.rehydrate(
                "SUB", "AG", "Agency Name", "nit", "address", "phone", "dane",
                "highlight", "pins", "ccp", "ccpid", "ccs", "ccsid", "pcp", "pcpid", "pcs", "pcsid",
                "description", "A", null, null, "user"
        );

        when(repository.save(any())).thenAnswer(invocation -> {
            AgencyEntity entity = invocation.getArgument(0);
            assertThat(entity.getCreatedAt()).isNotNull();
            assertThat(entity.getUpdatedAt()).isNotNull();
            return entity;
        });

        StepVerifier.create(jpaAgencyRepository.save(agency))
                .assertNext(saved -> {
                    assertThat(saved.subtypeCode()).isEqualTo("SUB");
                    assertThat(saved.agencyCode()).isEqualTo("AG");
                    assertThat(saved.createdAt()).isNotNull();
                    assertThat(saved.updatedAt()).isNotNull();
                })
                .verifyComplete();

        ArgumentCaptor<AgencyEntity> captor = ArgumentCaptor.forClass(AgencyEntity.class);
        Mockito.verify(repository).save(captor.capture());
        AgencyEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getId()).isEqualTo(new AgencyEntityId("SUB", "AG"));
        assertThat(savedEntity.getCreatedAt()).isInstanceOf(OffsetDateTime.class);
        assertThat(savedEntity.getUpdatedAt()).isInstanceOf(OffsetDateTime.class);
    }

    @Test
    void findByPkReturnsMappedEntity() {
        AgencyJpaRepository repository = mock(AgencyJpaRepository.class);
        JpaAgencyRepository jpaAgencyRepository = new JpaAgencyRepository(repository, new NoOpTransactionManager());

        AgencyEntity entity = buildEntity("AG");
        when(repository.findById(new AgencyEntityId("SUB", "AG"))).thenReturn(Optional.of(entity));

        StepVerifier.create(jpaAgencyRepository.findByPk("SUB", "AG"))
                .assertNext(found -> {
                    assertThat(found.subtypeCode()).isEqualTo("SUB");
                    assertThat(found.agencyCode()).isEqualTo("AG");
                    assertThat(found.name()).isEqualTo("Agency Name");
                })
                .verifyComplete();
    }

    @Test
    void findByPkEmitsErrorWhenMissing() {
        AgencyJpaRepository repository = mock(AgencyJpaRepository.class);
        JpaAgencyRepository jpaAgencyRepository = new JpaAgencyRepository(repository, new NoOpTransactionManager());

        when(repository.findById(new AgencyEntityId("SUB", "MISS"))).thenReturn(Optional.empty());

        StepVerifier.create(jpaAgencyRepository.findByPk("SUB", "MISS"))
                .expectErrorSatisfies(throwable -> assertThat(throwable)
                        .isInstanceOf(NoSuchElementException.class)
                        .hasMessageContaining("subtype=SUB agency=MISS"))
                .verify();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void findAllBuildsSpecificationAndPaging() {
        AgencyJpaRepository repository = mock(AgencyJpaRepository.class);
        JpaAgencyRepository jpaAgencyRepository = new JpaAgencyRepository(repository, new NoOpTransactionManager());

        AgencyEntity first = buildEntity("001");
        AgencyEntity second = buildEntity("002");

        ArgumentCaptor<Specification<AgencyEntity>> specCaptor =
                ArgumentCaptor.forClass((Class) Specification.class);

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);

        when(repository.findAll(specCaptor.capture(), pageRequestCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of(first, second)));

        StepVerifier.create(jpaAgencyRepository.findAll("SUB", "A", "agency", -1, 0).collectList())
                .assertNext(agencies -> {
                    assertThat(agencies).hasSize(2);
                    assertThat(agencies.get(0).agencyCode()).isEqualTo("001");
                    assertThat(agencies.get(1).agencyCode()).isEqualTo("002");
                })
                .verifyComplete();

        PageRequest pageRequest = pageRequestCaptor.getValue();
        assertThat(pageRequest.getPageNumber()).isEqualTo(0);
        assertThat(pageRequest.getPageSize()).isEqualTo(1);

        Specification<AgencyEntity> specification = specCaptor.getValue();
        assertThat(specification).isNotNull();

        Root<AgencyEntity> root = mock(Root.class);
        CriteriaQuery<AgencyEntity> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<?> idPath = mock(Path.class);
        Path<String> subtypePath = mock(Path.class);
        Path<String> agencyCodePath = mock(Path.class);
        Path<String> statusPath = mock(Path.class);
        Path<String> namePath = mock(Path.class);
        Expression<String> upperName = mock(Expression.class);
        Expression<String> upperCode = mock(Expression.class);
        Predicate subtypePredicate = mock(Predicate.class);
        Predicate statusPredicate = mock(Predicate.class);
        Predicate namePredicate = mock(Predicate.class);
        Predicate codePredicate = mock(Predicate.class);
        Predicate orPredicate = mock(Predicate.class);
        Predicate andPredicate = mock(Predicate.class);
        AtomicReference<String> likePattern = new AtomicReference<>();

        when(root.get("id")).thenReturn((Path) idPath);
        when(idPath.get("subtypeCode")).thenReturn((Path) subtypePath);
        when(idPath.get("agencyCode")).thenReturn((Path) agencyCodePath);
        when(root.get("status")).thenReturn((Path) statusPath);
        when(root.get("name")).thenReturn((Path) namePath);
        when(criteriaBuilder.equal(subtypePath, "SUB")).thenReturn(subtypePredicate);
        when(criteriaBuilder.equal(statusPath, "A")).thenReturn(statusPredicate);
        when(criteriaBuilder.upper(namePath)).thenReturn(upperName);
        when(criteriaBuilder.upper(agencyCodePath)).thenReturn(upperCode);
        when(criteriaBuilder.like(any(), anyString()))
                .thenAnswer(invocation -> {
                    likePattern.set(invocation.getArgument(1));
                    return invocation.getArgument(0) == upperName ? namePredicate : codePredicate;
                });
        when(criteriaBuilder.or(namePredicate, codePredicate)).thenReturn(orPredicate);
        when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(andPredicate);

        Predicate result = specification.toPredicate(root, query, criteriaBuilder);

        assertThat(result).isEqualTo(andPredicate);
        assertThat(likePattern.get()).isEqualTo("%AGENCY%");

        Mockito.verify(criteriaBuilder, times(1)).equal(subtypePath, "SUB");
        Mockito.verify(criteriaBuilder, times(1)).equal(statusPath, "A");
        Mockito.verify(criteriaBuilder, times(1)).or(namePredicate, codePredicate);
    }

    private AgencyEntity buildEntity(String agencyCode) {
        AgencyEntity entity = new AgencyEntity();
        entity.setId(new AgencyEntityId("SUB", agencyCode));
        entity.setName("Agency Name");
        entity.setStatus("A");
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        entity.setUpdatedBy("user");
        return entity;
    }

    private static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @NotNull
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(@NotNull Object transaction, @NotNull TransactionDefinition definition) {
            // no-op
        }

        @Override
        protected void doCommit(@NotNull DefaultTransactionStatus status) {
            // no-op
        }

        @Override
        protected void doRollback(@NotNull DefaultTransactionStatus status) {
            // no-op
        }
    }
}

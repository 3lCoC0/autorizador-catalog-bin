package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.CommercePlanEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.ValidationMapEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.CommercePlanJpaRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.SubtypeJpaRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.ValidationJpaRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.ValidationMapJpaRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaSpecificationAndPlanErrorTests {

    @SuppressWarnings("unchecked")
    @Test
    void validationMapBuildSpecificationCreatesPredicates() throws Exception {
        ValidationMapJpaRepository repository = mock(ValidationMapJpaRepository.class);
        SubtypeJpaRepository subtypeRepository = mock(SubtypeJpaRepository.class);
        ValidationJpaRepository validationRepository = mock(ValidationJpaRepository.class);
        var specification = getValidationMapEntitySpecification(repository, subtypeRepository, validationRepository);

        Root<ValidationMapEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<String> subtypePath = mock(Path.class);
        Path<String> binPath = mock(Path.class);
        Path<String> statusPath = mock(Path.class);
        Predicate subtypePredicate = mock(Predicate.class);
        Predicate binPredicate = mock(Predicate.class);
        Predicate statusPredicate = mock(Predicate.class);
        Predicate combinedPredicate = mock(Predicate.class);

        when(root.<String>get("subtypeCode")).thenReturn(subtypePath);
        when(root.<String>get("bin")).thenReturn(binPath);
        when(root.<String>get("status")).thenReturn(statusPath);
        when(cb.equal(subtypePath, "SUB")).thenReturn(subtypePredicate);
        when(cb.equal(binPath, "123456")).thenReturn(binPredicate);
        when(cb.equal(statusPath, "A")).thenReturn(statusPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(combinedPredicate);

        Predicate result = specification.toPredicate(root, query, cb);

        assertThat(result).isEqualTo(combinedPredicate);

        ArgumentCaptor<Predicate[]> predicateCaptor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(predicateCaptor.capture());
        Predicate[] predicates = predicateCaptor.getValue();
        assertThat(predicates).containsExactly(subtypePredicate, binPredicate, statusPredicate);
    }

    @SuppressWarnings("unchecked")
    private static Specification<ValidationMapEntity> getValidationMapEntitySpecification(
            ValidationMapJpaRepository repository,
            SubtypeJpaRepository subtypeRepository,
            ValidationJpaRepository validationRepository
    ) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        JpaValidationMapRepository jpaRepository = new JpaValidationMapRepository(
                repository, subtypeRepository, validationRepository, new NoOpTransactionManager());

        Method buildSpecification = JpaValidationMapRepository.class
                .getDeclaredMethod("buildSpecification", String.class, String.class, String.class);
        buildSpecification.setAccessible(true);

        return (Specification<ValidationMapEntity>)
                buildSpecification.invoke(jpaRepository, "SUB", "123456", "A");
    }

    @SuppressWarnings("unchecked")
    @Test
    void commercePlanBuildSpecificationAddsStatusAndSearchFilters() throws Exception {
        CommercePlanJpaRepository repository = mock(CommercePlanJpaRepository.class);
        var specification = getCommercePlanEntitySpecification(repository);

        Root<CommercePlanEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<String> statusPath = mock(Path.class);
        Path<String> codePath = mock(Path.class);
        Path<String> namePath = mock(Path.class);
        Expression<String> upperCode = mock(Expression.class);
        Expression<String> upperName = mock(Expression.class);
        Predicate statusPredicate = mock(Predicate.class);
        Predicate codeLikePredicate = mock(Predicate.class);
        Predicate nameLikePredicate = mock(Predicate.class);
        Predicate orPredicate = mock(Predicate.class);
        Predicate combinedPredicate = mock(Predicate.class);

        when(root.<String>get("status")).thenReturn(statusPath);
        when(root.<String>get("planCode")).thenReturn(codePath);
        when(root.<String>get("planName")).thenReturn(namePath);
        when(cb.equal(statusPath, "A")).thenReturn(statusPredicate);
        when(cb.upper(codePath)).thenReturn(upperCode);
        when(cb.upper(namePath)).thenReturn(upperName);
        when(cb.like(upperCode, "%PLAN%"))
                .thenReturn(codeLikePredicate);
        when(cb.like(upperName, "%PLAN%"))
                .thenReturn(nameLikePredicate);
        when(cb.or(codeLikePredicate, nameLikePredicate)).thenReturn(orPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(combinedPredicate);

        Predicate result = specification.toPredicate(root, query, cb);

        assertThat(result).isEqualTo(combinedPredicate);

        verify(cb).like(upperCode, "%PLAN%");
        verify(cb).like(upperName, "%PLAN%");
        verify(cb).or(codeLikePredicate, nameLikePredicate);
    }

    @SuppressWarnings("unchecked")
    private static Specification<CommercePlanEntity> getCommercePlanEntitySpecification(
            CommercePlanJpaRepository repository
    ) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        JpaCommercePlanRepository commercePlanRepository =
                new JpaCommercePlanRepository(repository, new NoOpTransactionManager());

        Method buildSpecification = JpaCommercePlanRepository.class
                .getDeclaredMethod("buildSpecification", String.class, String.class);
        buildSpecification.setAccessible(true);

        return (Specification<CommercePlanEntity>)
                buildSpecification.invoke(commercePlanRepository, "A", " plan ");
    }

    @Test
    void detectsPlanCodeLengthErrorsAcrossCauseChain() throws Exception {
        CommercePlanJpaRepository repository = mock(CommercePlanJpaRepository.class);
        JpaCommercePlanRepository commercePlanRepository = new JpaCommercePlanRepository(repository, new NoOpTransactionManager());

        Method isPlanCodeLengthError = JpaCommercePlanRepository.class
                .getDeclaredMethod("isPlanCodeLengthError", Throwable.class);
        isPlanCodeLengthError.setAccessible(true);

        Throwable wrapped = new DataAccessResourceFailureException("top level", new RuntimeException("ORA-00910 length error"));
        boolean detected = (boolean) isPlanCodeLengthError.invoke(commercePlanRepository, wrapped);
        boolean notDetected = (boolean) isPlanCodeLengthError.invoke(commercePlanRepository, new RuntimeException("other"));

        assertThat(detected).isTrue();
        assertThat(notDetected).isFalse();
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

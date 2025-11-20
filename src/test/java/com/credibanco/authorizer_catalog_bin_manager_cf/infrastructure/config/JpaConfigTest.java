package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JpaConfigTest {

    @Test
    void hikariConfigCreatesNewInstance() {
        JpaConfig config = new JpaConfig();

        HikariConfig hikariConfig = config.hikariConfig();

        assertNotNull(hikariConfig);
    }

    @Test
    void dataSourceCopiesPropertiesIntoConfig() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl("jdbc:h2:mem:testdb");
        properties.setUsername("user");
        properties.setPassword("pass");
        HikariConfig hikariConfig = new HikariConfig();
        JpaConfig config = new JpaConfig();

        try (MockedConstruction<HikariDataSource> mocked = Mockito.mockConstruction(
                HikariDataSource.class,
                (mock, context) -> assertSame(hikariConfig, context.arguments().get(0)))) {
            DataSource dataSource = config.dataSource(properties, hikariConfig);

            assertSame(mocked.constructed().get(0), dataSource);
        }

        assertEquals("jdbc:h2:mem:testdb", hikariConfig.getJdbcUrl());
        assertEquals("user", hikariConfig.getUsername());
        assertEquals("pass", hikariConfig.getPassword());
    }

    @Test
    void entityManagerFactoryConfiguresVendorAndProperties() {
        DataSource dataSource = mock(DataSource.class);
        JpaProperties jpaProperties = new JpaProperties();
        jpaProperties.setShowSql(true);
        jpaProperties.setProperties(Map.of("hibernate.dialect", "org.hibernate.dialect.H2Dialect"));
        JpaConfig config = new JpaConfig();

        LocalContainerEntityManagerFactoryBean factory = config.entityManagerFactory(dataSource, jpaProperties);

        assertSame(dataSource, factory.getDataSource());
        assertTrue(factory.getJpaVendorAdapter() instanceof HibernateJpaVendorAdapter);
        HibernateJpaVendorAdapter adapter = (HibernateJpaVendorAdapter) factory.getJpaVendorAdapter();
        assertTrue((boolean) ReflectionTestUtils.invokeMethod(adapter, "isShowSql"));
        assertEquals(Map.of("hibernate.dialect", "org.hibernate.dialect.H2Dialect"), factory.getJpaPropertyMap());
    }

    @Test
    void transactionManagerWrapsEntityManagerFactory() {
        JpaConfig config = new JpaConfig();
        EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);

        JpaTransactionManager transactionManager = config.transactionManager(entityManagerFactory);

        assertSame(entityManagerFactory, transactionManager.getEntityManagerFactory());
    }

    @Test
    void reactiveTransactionManagerWrapsPlatformManager() {
        PlatformTransactionManager delegate = mock(PlatformTransactionManager.class);
        JpaConfig config = new JpaConfig();

        ReactiveTransactionManager transactionManager = config.reactiveTransactionManager(delegate);

        assertTrue(transactionManager instanceof JpaConfig.ReactivePlatformTransactionManagerAdapter);
    }

    @Test
    void transactionalOperatorUsesReactiveTransactionManager() {
        ReactiveTransactionManager transactionManager = mock(ReactiveTransactionManager.class);
        JpaConfig config = new JpaConfig();

        TransactionalOperator operator = config.transactionalOperator(transactionManager);

        assertNotNull(operator);
    }

    @Test
    void reactiveTransactionAdapterDelegatesStatusOperations() {
        TransactionStatus status = new DefaultTransactionStatus(null, false, false, false, false, null);
        Scheduler.Worker worker = Schedulers.single().createWorker();
        JpaConfig.ReactiveTransactionAdapter adapter = new JpaConfig.ReactiveTransactionAdapter(status, worker);

        assertFalse(adapter.isNewTransaction());
        assertFalse(adapter.isRollbackOnly());
        adapter.setRollbackOnly();
        assertTrue(adapter.isRollbackOnly());
        assertFalse(adapter.isCompleted());

        adapter.setRollbackOnly();
        worker.dispose();
    }

    @Test
    void reactiveTransactionManagerCommitsAndDisposesWorker() {
        PlatformTransactionManager delegate = mock(PlatformTransactionManager.class);
        TransactionStatus status = new DefaultTransactionStatus(null, true, false, false, false, null);
        Scheduler.Worker worker = Schedulers.single().createWorker();
        JpaConfig.ReactivePlatformTransactionManagerAdapter manager =
                new JpaConfig.ReactivePlatformTransactionManagerAdapter(delegate);
        JpaConfig.ReactiveTransactionAdapter adapter =
                new JpaConfig.ReactiveTransactionAdapter(status, worker);

        StepVerifier.create(manager.commit(adapter)).verifyComplete();
        verify(delegate).commit(status);
        assertTrue(worker.isDisposed());
    }

    @Test
    void reactiveTransactionManagerRollsBackAndDisposesWorker() {
        PlatformTransactionManager delegate = mock(PlatformTransactionManager.class);
        TransactionStatus status = new DefaultTransactionStatus(null, true, false, false, false, null);
        Scheduler.Worker worker = Schedulers.single().createWorker();
        JpaConfig.ReactivePlatformTransactionManagerAdapter manager =
                new JpaConfig.ReactivePlatformTransactionManagerAdapter(delegate);
        JpaConfig.ReactiveTransactionAdapter adapter =
                new JpaConfig.ReactiveTransactionAdapter(status, worker);

        StepVerifier.create(manager.rollback(adapter)).verifyComplete();
        verify(delegate).rollback(status);
        assertTrue(worker.isDisposed());
    }

    @Test
    void getReactiveTransactionUsesDefaultDefinitionWhenNull() {
        PlatformTransactionManager delegate = mock(PlatformTransactionManager.class);
        Mockito.when(delegate.getTransaction(Mockito.any(TransactionDefinition.class)))
                .thenReturn(new DefaultTransactionStatus(null, true, false, false, false, null));

        JpaConfig.ReactivePlatformTransactionManagerAdapter manager =
                new JpaConfig.ReactivePlatformTransactionManagerAdapter(delegate);

        Mono<org.springframework.transaction.ReactiveTransaction> txMono = manager.getReactiveTransaction(null);
        StepVerifier.create(txMono)
                .expectNextMatches(tx -> tx instanceof JpaConfig.ReactiveTransactionAdapter)
                .verifyComplete();

        verify(delegate).getTransaction(Mockito.argThat(def -> def instanceof DefaultTransactionDefinition));
    }

    @Test
    void getReactiveTransactionUsesProvidedDefinition() {
        PlatformTransactionManager delegate = mock(PlatformTransactionManager.class);
        TransactionDefinition customDefinition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS);
        Mockito.when(delegate.getTransaction(customDefinition))
                .thenReturn(new DefaultTransactionStatus(null, true, false, false, false, null));
        JpaConfig.ReactivePlatformTransactionManagerAdapter manager =
                new JpaConfig.ReactivePlatformTransactionManagerAdapter(delegate);

        Mono<org.springframework.transaction.ReactiveTransaction> txMono =
                manager.getReactiveTransaction(customDefinition);

        StepVerifier.create(txMono)
                .expectNextMatches(tx -> tx instanceof JpaConfig.ReactiveTransactionAdapter)
                .verifyComplete();

        verify(delegate).getTransaction(customDefinition);
    }

    @Test
    void commitFailsForNonAdapterTransactions() {
        PlatformTransactionManager delegate = mock(PlatformTransactionManager.class);
        JpaConfig.ReactivePlatformTransactionManagerAdapter manager =
                new JpaConfig.ReactivePlatformTransactionManagerAdapter(delegate);

        assertThrows(IllegalArgumentException.class,
                () -> manager.commit(mock(org.springframework.transaction.ReactiveTransaction.class)));
    }

    @Test
    void rollbackFailsForNonAdapterTransactions() {
        PlatformTransactionManager delegate = mock(PlatformTransactionManager.class);
        JpaConfig.ReactivePlatformTransactionManagerAdapter manager =
                new JpaConfig.ReactivePlatformTransactionManagerAdapter(delegate);

        assertThrows(IllegalArgumentException.class,
                () -> manager.rollback(mock(org.springframework.transaction.ReactiveTransaction.class)));
    }
}

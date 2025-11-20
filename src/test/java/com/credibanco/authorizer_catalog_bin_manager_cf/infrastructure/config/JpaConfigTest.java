package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JpaConfigTest {

    private JpaConfig config;

    @BeforeEach
    void setup() {
        config = new JpaConfig();
    }

    @AfterEach
    void tearDown() throws SQLException {
        try {
            DriverManager.deregisterDriver(DummyDriver.INSTANCE);
        } catch (SQLException ignored) {
            // Driver was not registered in this test
        }
    }

    @Test
    void hikariBeansAreCreatedAndConfigured() throws SQLException {
        DriverManager.registerDriver(DummyDriver.INSTANCE);
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl("jdbc:dummy");
        properties.setUsername("user");
        properties.setPassword("pwd");
        properties.setDriverClassName(DummyDriver.class.getName());

        HikariConfig hkConfig = config.hikariConfig();
        assertThrows(com.zaxxer.hikari.pool.HikariPool.PoolInitializationException.class,
                () -> config.dataSource(properties, hkConfig));
        assertThat(hkConfig.getJdbcUrl()).isEqualTo("jdbc:dummy");
        assertThat(hkConfig.getUsername()).isEqualTo("user");
    }

    @Test
    void entityManagerFactoryUsesJpaProperties() {
        JpaProperties jpaProperties = new JpaProperties();
        jpaProperties.setShowSql(true);
        jpaProperties.setProperties(Map.of("hibernate.dialect", "H2"));

        LocalContainerEntityManagerFactoryBean factoryBean = config.entityManagerFactory(mock(javax.sql.DataSource.class), jpaProperties);

        assertThat(factoryBean.getJpaPropertyMap()).containsEntry("hibernate.dialect", "H2");
        assertThat(factoryBean.getJpaVendorAdapter()).isNotNull();
    }

    @Test
    void transactionalInfrastructureBeansAreAvailable() {
        EntityManagerFactory emf = mock(EntityManagerFactory.class);
        PlatformTransactionManager ptm = mock(PlatformTransactionManager.class);

        assertThat(config.transactionManager(emf)).isNotNull();
        assertThat(config.reactiveTransactionManager(ptm)).isInstanceOf(JpaConfig.ReactivePlatformTransactionManagerAdapter.class);
        assertThat(config.transactionalOperator(config.reactiveTransactionManager(ptm))).isNotNull();
    }

    @Test
    void reactiveAdapterDelegatesLifecycleOperations() {
        PlatformTransactionManager delegate = mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        when(delegate.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
        when(status.isNewTransaction()).thenReturn(true);
        when(status.isRollbackOnly()).thenReturn(false);
        when(status.isCompleted()).thenReturn(false);

        JpaConfig.ReactivePlatformTransactionManagerAdapter adapter =
                new JpaConfig.ReactivePlatformTransactionManagerAdapter(delegate);

        Mono<org.springframework.transaction.ReactiveTransaction> txMono = adapter.getReactiveTransaction(null);

        org.springframework.transaction.ReactiveTransaction tx = txMono.block();
        assertThat(tx).isNotNull();
        assertThat(tx.isNewTransaction()).isTrue();
        tx.setRollbackOnly();
        verify(status).setRollbackOnly();

        StepVerifier.create(adapter.commit(tx)).verifyComplete();
        verify(delegate).commit(status);

        org.springframework.transaction.ReactiveTransaction secondTx = adapter.getReactiveTransaction(null).block();
        StepVerifier.create(adapter.rollback(secondTx)).verifyComplete();
        verify(delegate).rollback(status);
    }

    public static final class DummyDriver implements Driver {
        private static final DummyDriver INSTANCE = new DummyDriver();

        public DummyDriver() {
        }

        @Override
        public java.sql.Connection connect(String url, Properties info) {
            return null;
        }

        @Override
        public boolean acceptsURL(String url) {
            return true;
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }
    }
}

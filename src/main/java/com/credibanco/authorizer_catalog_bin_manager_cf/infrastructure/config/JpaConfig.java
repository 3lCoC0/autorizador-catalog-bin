package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
@EnableConfigurationProperties({DataSourceProperties.class, JpaProperties.class})
public class JpaConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariConfig hikariConfig() {
        return new HikariConfig();
    }

    @Bean
    public DataSource dataSource(DataSourceProperties properties, HikariConfig config) {
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        if (properties.getDriverClassName() != null) {
            config.setDriverClassName(properties.getDriverClassName());
        }
        return new HikariDataSource(config);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
                                                                       JpaProperties jpaProperties) {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setDatabase(Database.ORACLE);
        vendorAdapter.setShowSql(jpaProperties.isShowSql());

        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan("com.credibanco.authorizer_catalog_bin_manager_cf");
        factoryBean.setJpaVendorAdapter(vendorAdapter);

        Map<String, String> additionalProperties = jpaProperties.getProperties();
        if (additionalProperties != null && !additionalProperties.isEmpty()) {
            factoryBean.setJpaPropertyMap(additionalProperties);
        }
        return factoryBean;
    }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    public ReactiveTransactionManager reactiveTransactionManager(PlatformTransactionManager transactionManager) {
        return new ReactivePlatformTransactionManagerAdapter(transactionManager);
    }

    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }

    static final class ReactivePlatformTransactionManagerAdapter implements ReactiveTransactionManager {

        private final PlatformTransactionManager delegate;
        private final Scheduler scheduler = Schedulers.boundedElastic();

        ReactivePlatformTransactionManagerAdapter(PlatformTransactionManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public Mono<ReactiveTransaction> getReactiveTransaction(TransactionDefinition definition) {
            TransactionDefinition txDefinition =
                    definition != null ? definition : new DefaultTransactionDefinition();
            return Mono.defer(() -> {
                Scheduler.Worker worker = scheduler.createWorker();
                return Mono.<ReactiveTransaction>create(sink -> {
                            sink.onCancel(worker::dispose);
                            worker.schedule(() -> {
                                try {
                                    TransactionStatus status = delegate.getTransaction(txDefinition);
                                    sink.success(new ReactiveTransactionAdapter(status, worker));
                                } catch (Throwable ex) {
                                    worker.dispose();
                                    sink.error(ex);
                                }
                            });
                        });
            });
        }

        @Override
        public Mono<Void> commit(ReactiveTransaction transaction) {
            ReactiveTransactionAdapter adapter = asAdapter(transaction);
            return Mono.<Void>create(sink -> {
                        sink.onCancel(adapter::disposeWorker);
                        adapter.worker().schedule(() -> {
                            try {
                                delegate.commit(adapter.status());
                                sink.success();
                            } catch (Throwable ex) {
                                sink.error(ex);
                            } finally {
                                adapter.disposeWorker();
                            }
                        });
                    });
        }

        @Override
        public Mono<Void> rollback(ReactiveTransaction transaction) {
            ReactiveTransactionAdapter adapter = asAdapter(transaction);
            return Mono.<Void>create(sink -> {
                        sink.onCancel(adapter::disposeWorker);
                        adapter.worker().schedule(() -> {
                            try {
                                delegate.rollback(adapter.status());
                                sink.success();
                            } catch (Throwable ex) {
                                sink.error(ex);
                            } finally {
                                adapter.disposeWorker();
                            }
                        });
                    });
        }

        private ReactiveTransactionAdapter asAdapter(ReactiveTransaction transaction) {
            Assert.isInstanceOf(ReactiveTransactionAdapter.class, transaction,
                    "ReactiveTransaction must be created by ReactivePlatformTransactionManagerAdapter");
            return (ReactiveTransactionAdapter) transaction;
        }
    }

    static final class ReactiveTransactionAdapter implements ReactiveTransaction {

        private final TransactionStatus status;
        private final Scheduler.Worker worker;

        ReactiveTransactionAdapter(TransactionStatus status, Scheduler.Worker worker) {
            this.status = status;
            this.worker = worker;
        }

        private TransactionStatus status() {
            return status;
        }

        private Scheduler.Worker worker() {
            return worker;
        }

        private void disposeWorker() {
            worker.dispose();
        }

        @Override
        public boolean isNewTransaction() {
            return status.isNewTransaction();
        }

        @Override
        public boolean isRollbackOnly() {
            return status.isRollbackOnly();
        }

        @Override
        public void setRollbackOnly() {
            status.setRollbackOnly();
        }

        @Override
        public boolean isCompleted() {
            return status.isCompleted();
        }
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JpaConfigTest {

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
}

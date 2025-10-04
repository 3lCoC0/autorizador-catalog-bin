package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.JpaConfig;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CreateBinServiceTest {

    @Test
    void whenBinAlreadyExistsServiceEmitsAppException() {
        BinRepository repo = mock(BinRepository.class);
        PlatformTransactionManager platformTxManager = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new DefaultTransactionStatus(null, true, true, false, true, null);
            }

            @Override
            public void commit(TransactionStatus status) {
                // no-op for test
            }

            @Override
            public void rollback(TransactionStatus status) {
                // no-op for test
            }
        };
        TransactionalOperator operator = TransactionalOperator
                .create(new JpaConfig.ReactivePlatformTransactionManagerAdapter(platformTxManager));
        CreateBinService service = new CreateBinService(repo, operator);

        String bin = "123456";
        when(repo.existsById(bin)).thenReturn(Mono.just(true));

        StepVerifier.create(service.execute(bin, "Test", "DEBITO", "01", "01", "desc", "N", null, "tester"))
                .expectErrorSatisfies(error -> assertTrue(error instanceof AppException))
                .verify();

        verify(repo).existsById(bin);
        verifyNoMoreInteractions(repo);
    }
}

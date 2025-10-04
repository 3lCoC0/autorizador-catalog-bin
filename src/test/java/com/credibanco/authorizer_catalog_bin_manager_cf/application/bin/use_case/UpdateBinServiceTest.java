package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateBinServiceTest {

    private BinRepository binRepository;
    private TransactionalOperator txOperator;
    private UpdateBinService service;

    @BeforeEach
    void setUp() {
        binRepository = mock(BinRepository.class);
        txOperator = mock(TransactionalOperator.class);
        service = new UpdateBinService(binRepository, txOperator);

        when(txOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void whenBinMissingReturnBinNotFoundError() {
        String bin = "123456";
        when(binRepository.findById(bin)).thenReturn(Mono.empty());

        StepVerifier.create(service.execute(bin, "name", "type", "account",
                        "compensation", "desc", "N", null, "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.BIN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        Mockito.verify(binRepository).findById(bin);
        Mockito.verifyNoMoreInteractions(binRepository);
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChangeBinStatusServiceTest {

    private BinRepository repo;
    private TransactionalOperator txOperator;
    private ChangeBinStatusService service;

    @BeforeEach
    void setUp() {
        repo = mock(BinRepository.class);
        txOperator = mock(TransactionalOperator.class);
        service = new ChangeBinStatusService(repo, txOperator);

        when(txOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void whenStatusInvalidReturnError() {
        StepVerifier.create(service.execute("123456", "X", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.BIN_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();

        verifyNoInteractions(repo);
    }

    @Test
    void whenBinMissingReturnNotFound() {
        String bin = "123456";
        when(repo.findById(bin)).thenReturn(Mono.empty());

        StepVerifier.create(service.execute(bin, "A", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.BIN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(repo).findById(bin);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void whenDomainRejectsStatusReturnAppException() {
        String bin = "123456";
        Bin existing = Bin.createNew(bin, "name", "DEBITO", "01",
                "01", "desc", "N", null, "creator");
        when(repo.findById(bin)).thenReturn(Mono.just(existing));

        StepVerifier.create(service.execute(bin, "a", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.BIN_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();

        verify(repo).findById(bin);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void whenValidStatusPersistsUpdatedBin() {
        String bin = "123456";
        Bin existing = Bin.createNew(bin, "name", "DEBITO", "01",
                "01", "desc", "N", null, "creator");
        when(repo.findById(bin)).thenReturn(Mono.just(existing));
        when(repo.save(any(Bin.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute(bin, "I", "user"))
                .assertNext(updated -> {
                    assertEquals("I", updated.status());
                    assertEquals("user", updated.updatedBy());
                })
                .verifyComplete();

        verify(repo).findById(bin);
        verify(repo).save(any(Bin.class));
        verifyNoMoreInteractions(repo);
    }
}

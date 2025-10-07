package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

        StepVerifier.create(service.execute(bin, "name", "DEBITO", "01",
                        "compensation", "desc", "N", null, "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.BIN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(binRepository).findById(bin);
        verifyNoMoreInteractions(binRepository);
    }

    @Test
    void whenDomainValidationFailsReturnAppException() {
        String bin = "123456";
        Bin existing = Bin.createNew(bin, "name", "DEBITO", "01",
                "01", "desc", "N", null, "creator");
        when(binRepository.findById(bin)).thenReturn(Mono.just(existing));

        StepVerifier.create(service.execute(bin, "name", "DEBITO", "1",
                        "compensation", "desc", "N", null, "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.BIN_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();

        verify(binRepository).findById(bin);
        verifyNoMoreInteractions(binRepository);
    }

    @Test
    void whenBinFoundUpdatesAndReturnsSavedBin() {
        String bin = "123456";
        Bin existing = Bin.rehydrate(bin, "Old", "DEBITO", "01",
                "01", "desc", "A", OffsetDateTime.now().minusDays(1), OffsetDateTime.now().minusHours(1),
                "tester", "N", null);
        when(binRepository.findById(bin)).thenReturn(Mono.just(existing));

        ArgumentCaptor<Bin> savedCaptor = ArgumentCaptor.forClass(Bin.class);
        when(binRepository.save(any(Bin.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute(bin, "New", "CREDITO", "02",
                        "02", "updated", "N", null, "user"))
                .assertNext(updated -> {
                    assertEquals("New", updated.name());
                    assertEquals("CREDITO", updated.typeBin());
                    assertEquals("02", updated.typeAccount());
                    assertEquals("user", updated.updatedBy());
                    assertNotNull(updated.updatedAt());
                })
                .verifyComplete();

        verify(binRepository).findById(bin);
        verify(binRepository).save(savedCaptor.capture());
        assertEquals("New", savedCaptor.getValue().name());
        verifyNoMoreInteractions(binRepository);
    }
}

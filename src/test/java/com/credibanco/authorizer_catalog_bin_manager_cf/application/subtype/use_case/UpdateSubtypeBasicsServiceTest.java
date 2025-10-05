package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class UpdateSubtypeBasicsServiceTest {

    private SubtypeRepository subtypeRepository;
    private BinReadOnlyRepository binRepository;
    private TransactionalOperator txOperator;
    private UpdateSubtypeBasicsService service;

    @BeforeEach
    void setUp() {
        subtypeRepository = mock(SubtypeRepository.class);
        binRepository = mock(BinReadOnlyRepository.class);
        txOperator = mock(TransactionalOperator.class);
        service = new UpdateSubtypeBasicsService(subtypeRepository, binRepository, txOperator);
        when(txOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldReturnNotFoundWhenSubtypeMissing() {
        when(subtypeRepository.findByPk("123456", "SUB")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("123456", "SUB", "Name", "Desc", null, null, null, "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void shouldReturnErrorWhenBinConfigMissing() {
        Subtype current = buildSubtype("SUB", "123456", "01", "I");
        when(subtypeRepository.findByPk("123456", "SUB")).thenReturn(Mono.just(current));
        when(binRepository.getExtConfig("123456")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("123456", "SUB", "Name", "Desc", null, null, null, "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.BIN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void shouldReturnErrorWhenExtNormalizationFails() {
        Subtype current = buildSubtype("SUB", "123456", "01", "I");
        when(subtypeRepository.findByPk("123456", "SUB")).thenReturn(Mono.just(current));
        when(binRepository.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 1)));

        StepVerifier.create(service.execute("123456", "SUB", "Name", "Desc", null, null, "999", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void shouldCheckBinExtUniquenessWhenChanged() {
        Subtype current = buildSubtype("SUB", "123456", "01", "I");
        when(subtypeRepository.findByPk("123456", "SUB")).thenReturn(Mono.just(current));
        when(binRepository.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 2)));
        when(subtypeRepository.existsByBinAndExt("123456", "02")).thenReturn(Mono.just(true));

        StepVerifier.create(service.execute("123456", "SUB", "Name", "Desc", null, null, "02", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_ALREADY_EXISTS, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void shouldSkipUniquenessCheckWhenExtDoesNotChange() {
        Subtype current = buildSubtype("SUB", "123456", "01", "I");
        when(subtypeRepository.findByPk("123456", "SUB")).thenReturn(Mono.just(current));
        when(binRepository.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 2)));
        when(subtypeRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute("123456", "SUB", "New", "Desc", null, null, "01", "user"))
                .assertNext(updated -> {
                    assertEquals("New", updated.name());
                    assertEquals("01", updated.binExt());
                })
                .verifyComplete();

        verify(subtypeRepository, times(0)).existsByBinAndExt(any(), any());
    }

    @Test
    void shouldUpdateSubtypeWhenExtChanges() {
        Subtype current = buildSubtype("SUB", "123456", null, "I");
        when(subtypeRepository.findByPk("123456", "SUB")).thenReturn(Mono.just(current));
        when(binRepository.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 2)));
        when(subtypeRepository.existsByBinAndExt("123456", "01")).thenReturn(Mono.just(false));
        when(subtypeRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute("123456", "SUB", "New", "Desc", null, null, "01", "user"))
                .assertNext(updated -> {
                    assertEquals("01", updated.binExt());
                    assertEquals("12345601", updated.binEfectivo());
                    assertNotEquals(current.updatedAt(), updated.updatedAt());
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleBinsWithoutExtensionUsage() {
        Subtype current = buildSubtype("SUB", "123456", null, "I");
        when(subtypeRepository.findByPk("123456", "SUB")).thenReturn(Mono.just(current));
        when(binRepository.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("N", null)));
        when(subtypeRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute("123456", "SUB", "New", "Desc", null, null, "99", "user"))
                .assertNext(updated -> {
                    assertEquals("New", updated.name());
                    assertEquals(current.binExt(), updated.binExt());
                })
                .verifyComplete();

        verify(subtypeRepository, times(0)).existsByBinAndExt(any(), any());
        verify(binRepository).getExtConfig("123456");
        verifyNoMoreInteractions(binRepository);
    }

    private static Subtype buildSubtype(String code, String bin, String binExt, String status) {
        OffsetDateTime now = OffsetDateTime.now();
        return Subtype.rehydrate(code, bin, "Name", "Desc", status, null, null, binExt,
                Subtype.computeBinEfectivo(bin, binExt), 1L, now.minusDays(1), now, "user");
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.IdTypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CreateSubtypeServiceTest {

    private SubtypeRepository subtypeRepository;
    private BinReadOnlyRepository binRepository;
    private IdTypeReadOnlyRepository idTypeRepository;
    private TransactionalOperator txOperator;
    private CreateSubtypeService service;

    @BeforeEach
    void setUp() {
        subtypeRepository = mock(SubtypeRepository.class);
        binRepository = mock(BinReadOnlyRepository.class);
        idTypeRepository = mock(IdTypeReadOnlyRepository.class);
        txOperator = mock(TransactionalOperator.class);

        service = new CreateSubtypeService(subtypeRepository, binRepository, idTypeRepository, txOperator);

        when(txOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void whenBinConfigMissingReturnBinNotFoundError() {
        String bin = "123456";
        when(binRepository.getExtConfig(bin)).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("SUB", bin, "Subtype", "desc",
                        null, null, null, "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.BIN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(binRepository).getExtConfig(bin);
        verifyNoMoreInteractions(binRepository, subtypeRepository, idTypeRepository);
    }

    @Test
    void whenOwnerIdTypeDoesNotExistShouldReturnInvalidDataError() {
        String bin = "123456";
        when(binRepository.getExtConfig(bin)).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 2)));
        when(idTypeRepository.existsById("CC")).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute("SUB", bin, "Subtype", "desc",
                        "CC", "123", "1", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    AppException appException = (AppException) error;
                    assertEquals(AppError.SUBTYPE_INVALID_DATA, appException.getError());
                    assertTrue(appException.getMessage().contains("OWNER_ID_TYPE"));
                })
                .verify();

        verify(idTypeRepository).existsById("CC");
    }

    @Test
    void whenExtNormalizationFailsShouldReturnInvalidDataError() {
        String bin = "123456";
        when(binRepository.getExtConfig(bin)).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 1)));
        when(idTypeRepository.existsById("CC")).thenReturn(Mono.just(true));

        StepVerifier.create(service.execute("SUB", bin, "Subtype", "desc",
                        "CC", "123", "123", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();

        verify(idTypeRepository).existsById("CC");
        verifyNoMoreInteractions(subtypeRepository);
    }

    @Test
    void whenBinMasterAlreadyExistsShouldReturnCollisionError() {
        String bin = "123456";
        when(binRepository.getExtConfig(bin)).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 2)));
        when(idTypeRepository.existsById("CC")).thenReturn(Mono.just(true));
        when(binRepository.existsById("12345612")).thenReturn(Mono.just(true));
        when(subtypeRepository.existsByPk(bin, "SUB")).thenReturn(Mono.just(false));
        when(subtypeRepository.existsByBinAndExt(bin, "12")).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute("SUB", bin, "Subtype", "desc",
                        "CC", "123", "12", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.BIN_ALREADY_EXISTS, ((AppException) error).getError());
                })
                .verify();

        verify(binRepository).existsById("12345612");
    }

    @Test
    void whenSubtypeAlreadyExistsShouldReturnError() {
        String bin = "123456";
        when(binRepository.getExtConfig(bin)).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 2)));
        when(idTypeRepository.existsById("CC")).thenReturn(Mono.just(true));
        when(binRepository.existsById("12345612")).thenReturn(Mono.just(false));
        when(subtypeRepository.existsByPk(bin, "SUB")).thenReturn(Mono.just(true));
        when(subtypeRepository.existsByBinAndExt(bin, "12")).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute("SUB", bin, "Subtype", "desc",
                        "CC", "123", "12", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_ALREADY_EXISTS, ((AppException) error).getError());
                })
                .verify();

        verify(subtypeRepository).existsByPk(bin, "SUB");
    }

    @Test
    void whenBinExtAlreadyUsedShouldReturnError() {
        String bin = "123456";
        when(binRepository.getExtConfig(bin)).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 2)));
        when(idTypeRepository.existsById("CC")).thenReturn(Mono.just(true));
        when(binRepository.existsById("12345612")).thenReturn(Mono.just(false));
        when(subtypeRepository.existsByPk(bin, "SUB")).thenReturn(Mono.just(false));
        when(subtypeRepository.existsByBinAndExt(bin, "12")).thenReturn(Mono.just(true));

        StepVerifier.create(service.execute("SUB", bin, "Subtype", "desc",
                        "CC", "123", "12", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_ALREADY_EXISTS, ((AppException) error).getError());
                })
                .verify();

        verify(subtypeRepository).existsByBinAndExt(bin, "12");
    }

    @Test
    void whenBinDoesNotUseExtensionsShouldIgnoreBinExtValue() {
        String bin = "654321";
        when(binRepository.getExtConfig(bin)).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("N", null)));
        when(idTypeRepository.existsById(null)).thenReturn(Mono.just(true));
        when(binRepository.existsById(bin)).thenReturn(Mono.just(false));
        when(subtypeRepository.existsByPk(bin, "SUB")).thenReturn(Mono.just(false));
        when(subtypeRepository.existsByBinAndExt(eq(bin), any())).thenReturn(Mono.just(false));
        when(subtypeRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute("SUB", bin, "Subtype", "desc",
                        null, null, "99", "user"))
                .assertNext(subtype -> {
                    assertEquals("SUB", subtype.subtypeCode());
                    assertEquals(bin, subtype.bin());
                    assertNull(subtype.binExt());
                })
                .verifyComplete();

        verify(subtypeRepository, times(0)).existsByBinAndExt(any(), any());
    }

    @Test
    void whenCreationSucceedsShouldPersistSubtype() {
        String bin = "123456";
        when(binRepository.getExtConfig(bin)).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 2)));
        when(idTypeRepository.existsById("CC")).thenReturn(Mono.just(true));
        when(binRepository.existsById("12345612")).thenReturn(Mono.just(false));
        when(subtypeRepository.existsByPk(bin, "SUB")).thenReturn(Mono.just(false));
        when(subtypeRepository.existsByBinAndExt(bin, "12")).thenReturn(Mono.just(false));
        when(subtypeRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute("SUB", bin, "Subtype", "desc",
                        "CC", "123", "12", "user"))
                .assertNext(subtype -> {
                    assertEquals("SUB", subtype.subtypeCode());
                    assertEquals("12345612", subtype.binEfectivo());
                    assertNotNull(subtype.updatedAt());
                })
                .verifyComplete();

        ArgumentCaptor<String> extCaptor = ArgumentCaptor.forClass(String.class);
        verify(subtypeRepository).existsByBinAndExt(eq(bin), extCaptor.capture());
        assertEquals("12", extCaptor.getValue());
    }
}

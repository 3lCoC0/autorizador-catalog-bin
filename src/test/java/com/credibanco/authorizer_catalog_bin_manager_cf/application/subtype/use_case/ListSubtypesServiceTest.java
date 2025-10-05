package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListSubtypesServiceTest {

    private SubtypeRepository subtypeRepository;
    private BinReadOnlyRepository binRepository;
    private ListSubtypesService service;

    @BeforeEach
    void setUp() {
        subtypeRepository = mock(SubtypeRepository.class);
        binRepository = mock(BinReadOnlyRepository.class);
        service = new ListSubtypesService(subtypeRepository, binRepository);
    }

    @Test
    void shouldFailWhenPageOrSizeInvalid() {
        StepVerifier.create(service.execute("123456", null, null, -1, 10))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();

        StepVerifier.create(service.execute("123456", null, null, 0, 0))
                .expectError(AppException.class)
                .verify();

        verifyNoInteractions(subtypeRepository, binRepository);
    }

    @Test
    void shouldValidateBinExistenceWhenFilterPresent() {
        when(binRepository.existsById("123456")).thenReturn(Mono.just(true));
        Subtype subtype = Subtype.createNew("SUB", "123456", "Name", "Desc", null, null, null, "user");
        when(subtypeRepository.findAll("123456", "S", "A", 0, 5)).thenReturn(Flux.fromIterable(List.of(subtype)));

        StepVerifier.create(service.execute("123456", "S", "A", 0, 5))
                .expectNext(subtype)
                .verifyComplete();

        verify(binRepository).existsById("123456");
    }

    @Test
    void shouldReturnErrorWhenBinFilterNotFound() {
        when(binRepository.existsById("123456")).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute("123456", null, null, 0, 10))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.BIN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void shouldListWithoutBinFilter() {
        Subtype subtype1 = Subtype.createNew("S1", "123456", "Name1", "Desc1", null, null, null, "user");
        Subtype subtype2 = Subtype.createNew("S2", "123456", "Name2", "Desc2", null, null, null, "user");
        when(subtypeRepository.findAll(null, null, null, 1, 2)).thenReturn(Flux.fromIterable(List.of(subtype1, subtype2)));

        StepVerifier.create(service.execute(null, null, null, 1, 2))
                .expectNext(subtype1)
                .expectNext(subtype2)
                .verifyComplete();

        verify(subtypeRepository).findAll(null, null, null, 1, 2);
        verifyNoInteractions(binRepository);
    }
}

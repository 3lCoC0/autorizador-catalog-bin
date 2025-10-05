package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class GetBinServiceTest {

    private BinRepository repo;
    private GetBinService service;

    @BeforeEach
    void setUp() {
        repo = mock(BinRepository.class);
        service = new GetBinService(repo);
    }

    @Test
    void whenBinNotFoundReturnAppException() {
        String bin = "123456";
        when(repo.findById(bin)).thenReturn(Mono.empty());

        StepVerifier.create(service.execute(bin))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.BIN_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();

        verify(repo).findById(bin);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void whenBinExistsReturnAggregate() {
        String bin = "123456";
        Bin existing = Bin.createNew(bin, "name", "DEBITO", "01", "01", "desc", "N", null, "creator");
        when(repo.findById(bin)).thenReturn(Mono.just(existing));

        StepVerifier.create(service.execute(bin))
                .expectNext(existing)
                .verifyComplete();

        verify(repo).findById(bin);
        verifyNoMoreInteractions(repo);
    }
}

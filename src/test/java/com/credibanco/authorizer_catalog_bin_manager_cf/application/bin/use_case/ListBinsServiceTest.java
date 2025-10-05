package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ListBinsServiceTest {

    private BinRepository repo;
    private ListBinsService service;

    @BeforeEach
    void setUp() {
        repo = mock(BinRepository.class);
        service = new ListBinsService(repo);
    }

    @Test
    void whenPageOrSizeInvalidReturnError() {
        StepVerifier.create(service.execute(-1, 0))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.BIN_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();

        verifyNoInteractions(repo);
    }

    @Test
    void whenParametersValidReturnFluxFromRepository() {
        Bin bin1 = Bin.createNew("123456", "One", "DEBITO", "01", "01", "desc", "N", null, "user");
        Bin bin2 = Bin.createNew("123457", "Two", "DEBITO", "01", "01", "desc", "N", null, "user");
        when(repo.findAll(0, 2)).thenReturn(Flux.just(bin1, bin2));

        StepVerifier.create(service.execute(0, 2))
                .expectNext(bin1)
                .expectNext(bin2)
                .verifyComplete();

        verify(repo).findAll(0, 2);
        verifyNoMoreInteractions(repo);
    }
}

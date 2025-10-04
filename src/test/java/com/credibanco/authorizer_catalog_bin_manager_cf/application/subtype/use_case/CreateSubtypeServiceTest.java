package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.IdTypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
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

        Mockito.verify(binRepository).getExtConfig(bin);
        Mockito.verifyNoMoreInteractions(binRepository, subtypeRepository, idTypeRepository);
    }
}

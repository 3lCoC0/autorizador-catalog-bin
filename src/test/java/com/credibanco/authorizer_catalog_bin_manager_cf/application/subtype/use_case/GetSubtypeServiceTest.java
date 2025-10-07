package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetSubtypeServiceTest {

    private SubtypeRepository subtypeRepository;
    private GetSubtypeService service;

    @BeforeEach
    void setUp() {
        subtypeRepository = mock(SubtypeRepository.class);
        service = new GetSubtypeService(subtypeRepository);
    }

    @Test
    void shouldReturnSubtypeWhenFound() {
        Subtype subtype = Subtype.createNew("SUB", "123456", "Name", "Desc", null, null, null, "user");
        when(subtypeRepository.findByPk("123456", "SUB")).thenReturn(Mono.just(subtype));

        StepVerifier.create(service.execute("123456", "SUB"))
                .expectNext(subtype)
                .verifyComplete();

        verify(subtypeRepository).findByPk("123456", "SUB");
    }

    @Test
    void shouldReturnNotFoundErrorWhenMissing() {
        when(subtypeRepository.findByPk("123456", "SUB")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("123456", "SUB"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.AgencyReadOnlyRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChangeSubtypeStatusServiceTest {

    private SubtypeRepository subtypeRepository;
    private AgencyReadOnlyRepository agencyRepository;
    private TransactionalOperator txOperator;
    private ChangeSubtypeStatusService service;

    @BeforeEach
    void setUp() {
        subtypeRepository = mock(SubtypeRepository.class);
        agencyRepository = mock(AgencyReadOnlyRepository.class);
        txOperator = mock(TransactionalOperator.class);
        service = new ChangeSubtypeStatusService(subtypeRepository, agencyRepository, txOperator);
        when(txOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldFailWhenStatusInvalid() {
        StepVerifier.create(service.execute("123456", "SUB", "X", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void shouldFailWhenSubtypeNotFound() {
        when(subtypeRepository.findByPk("123456", "SUB")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("123456", "SUB", "A", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void shouldValidateAgenciesWhenActivating() {
        Subtype current = buildSubtype("SUB", "123456", "I");
        when(subtypeRepository.findByPk("123456", "SUB")).thenReturn(Mono.just(current));
        when(agencyRepository.countActiveBySubtypeCode("SUB")).thenReturn(Mono.just(0L));

        StepVerifier.create(service.execute("123456", "SUB", "A", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_ACTIVATE_REQUIRES_AGENCY, ((AppException) error).getError());
                })
                .verify();
    }

    @Test
    void shouldActivateWhenAgenciesPresent() {
        Subtype current = buildSubtype("SUB", "123456", "I");
        when(subtypeRepository.findByPk("123456", "SUB")).thenReturn(Mono.just(current));
        when(agencyRepository.countActiveBySubtypeCode("SUB")).thenReturn(Mono.just(2L));
        when(subtypeRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute("123456", "SUB", "A", "user"))
                .assertNext(updated -> {
                    assertEquals("A", updated.status());
                    assertEquals("SUB", updated.subtypeCode());
                })
                .verifyComplete();

        verify(agencyRepository).countActiveBySubtypeCode("SUB");
    }

    @Test
    void shouldDeactivateWithoutAgencyCheck() {
        Subtype current = buildSubtype("SUB", "123456", "A");
        when(subtypeRepository.findByPk("123456", "SUB")).thenReturn(Mono.just(current));
        when(subtypeRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.execute("123456", "SUB", "I", "user"))
                .assertNext(updated -> assertEquals("I", updated.status()))
                .verifyComplete();

        verify(agencyRepository, times(0)).countActiveBySubtypeCode(any());
    }

    @Test
    void shouldMapIllegalArgumentToAppException() {
        Subtype current = buildSubtype("SUB", "123456", "A");
        when(subtypeRepository.findByPk("123456", "SUB")).thenReturn(Mono.just(current));
        when(subtypeRepository.save(any())).thenReturn(Mono.error(new IllegalArgumentException("invalid")));

        StepVerifier.create(service.execute("123456", "SUB", "I", "user"))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppException);
                    assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) error).getError());
                })
                .verify();
    }

    private static Subtype buildSubtype(String code, String bin, String status) {
        OffsetDateTime now = OffsetDateTime.now();
        return Subtype.rehydrate(code, bin, "Name", "Desc", status, null, null, null,
                Subtype.computeBinEfectivo(bin, null), 1L, now.minusDays(1), now, "user");
    }
}

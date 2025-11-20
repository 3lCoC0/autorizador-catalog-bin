package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgencyQueryServicesTest {

    private AgencyRepository repo;
    private SubtypeReadOnlyRepository subtypeRepo;
    private TransactionalOperator tx;

    private Agency agency;

    @BeforeEach
    void setUp() {
        repo = mock(AgencyRepository.class);
        subtypeRepo = mock(SubtypeReadOnlyRepository.class);
        tx = mock(TransactionalOperator.class);
        when(tx.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));

        agency = Agency.createNew("SUB", "01", "Main", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, "creator");
    }

    @Test
    void listRejectsInvalidPagination() {
        ListAgenciesService service = new ListAgenciesService(repo, subtypeRepo);

        StepVerifier.create(service.execute("SUB", "A", null, -1, 0))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.AGENCY_INVALID_DATA, ((AppException) err).getError());
                })
                .verify();
    }

    @Test
    void listWithoutSubtypeUsesRepositoryDirectly() {
        when(repo.findAll(null, "A", "search", 0, 5)).thenReturn(Flux.just(agency));
        ListAgenciesService service = new ListAgenciesService(repo, subtypeRepo);

        StepVerifier.create(service.execute(" ", "A", "search", 0, 5))
                .expectNext(agency)
                .verifyComplete();

        verify(repo).findAll(null, "A", "search", 0, 5);
        verifyNoInteractions(subtypeRepo);
    }

    @Test
    void listValidatesSubtypeExistence() {
        when(subtypeRepo.existsByCode("SUB")).thenReturn(Mono.just(false));
        ListAgenciesService service = new ListAgenciesService(repo, subtypeRepo);

        StepVerifier.create(service.execute("SUB", null, null, 0, 5))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) err).getError());
                })
                .verify();
    }

    @Test
    void listFiltersBySubtypeWhenExists() {
        when(subtypeRepo.existsByCode("SUB")).thenReturn(Mono.just(true));
        when(repo.findAll("SUB", null, null, 0, 1)).thenReturn(Flux.just(agency));
        ListAgenciesService service = new ListAgenciesService(repo, subtypeRepo);

        StepVerifier.create(service.execute("SUB", null, null, 0, 1))
                .expectNext(agency)
                .verifyComplete();

        verify(repo).findAll("SUB", null, null, 0, 1);
    }

    @Test
    void getFailsWhenSubtypeMissing() {
        when(subtypeRepo.existsByCode("SUB")).thenReturn(Mono.just(false));
        GetAgencyService service = new GetAgencyService(repo, subtypeRepo);

        StepVerifier.create(service.execute("SUB", "01"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) err).getError());
                })
                .verify();
    }

    @Test
    void getFailsWhenAgencyMissing() {
        when(subtypeRepo.existsByCode("SUB")).thenReturn(Mono.just(true));
        when(repo.findByPk("SUB", "99")).thenReturn(Mono.empty());
        GetAgencyService service = new GetAgencyService(repo, subtypeRepo);

        StepVerifier.create(service.execute("SUB", "99"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.AGENCY_NOT_FOUND, ((AppException) err).getError());
                })
                .verify();
    }

    @Test
    void getReturnsAgencyWhenFound() {
        when(subtypeRepo.existsByCode("SUB")).thenReturn(Mono.just(true));
        when(repo.findByPk("SUB", "01")).thenReturn(Mono.just(agency));
        GetAgencyService service = new GetAgencyService(repo, subtypeRepo);

        StepVerifier.create(service.execute("SUB", "01"))
                .expectNextMatches(found -> found.agencyCode().equals("01"))
                .verifyComplete();
    }

    @Test
    void changeStatusFailsWhenSubtypeMissing() {
        when(repo.findByPk("SUB", "01")).thenReturn(Mono.just(agency));
        when(subtypeRepo.existsByCode("SUB")).thenReturn(Mono.just(false));
        ChangeAgencyStatusService service = new ChangeAgencyStatusService(repo, subtypeRepo, tx);

        StepVerifier.create(service.execute("SUB", "01", "A", "actor"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) err).getError());
                })
                .verify();
    }

    @Test
    void changeStatusCompletesWhenAnotherActiveExists() {
        when(repo.findByPk("SUB", "01")).thenReturn(Mono.just(agency));
        when(subtypeRepo.existsByCode("SUB")).thenReturn(Mono.just(true));
        when(repo.existsAnotherActive("SUB", "01")).thenReturn(Mono.just(true));
        when(repo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        ChangeAgencyStatusService service = new ChangeAgencyStatusService(repo, subtypeRepo, tx);

        StepVerifier.create(service.execute("SUB", "01", "I", "actor"))
                .assertNext(updated -> {
                    assertEquals("I", updated.status());
                    assertThat(updated.updatedAt()).isNotNull();
                    assertEquals("actor", updated.updatedBy());
                })
                .verifyComplete();

        verify(repo).save(any(Agency.class));
    }

    @Test
    void changeStatusMapsIllegalArgumentToAppException() {
        when(repo.findByPk("SUB", "01")).thenReturn(Mono.error(new IllegalArgumentException("bad")));
        when(subtypeRepo.existsByCode("SUB")).thenReturn(Mono.just(true));
        ChangeAgencyStatusService service = new ChangeAgencyStatusService(repo, subtypeRepo, tx);

        StepVerifier.create(service.execute("SUB", "01", "A", "actor"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.AGENCY_INVALID_DATA, ((AppException) err).getError());
                })
                .verify();
    }

    @Test
    void changeStatusFailsWhenAgencyNotFound() {
        when(repo.findByPk("SUB", "01")).thenReturn(Mono.empty());
        when(subtypeRepo.existsByCode("SUB")).thenReturn(Mono.just(true));
        ChangeAgencyStatusService service = new ChangeAgencyStatusService(repo, subtypeRepo, tx);

        StepVerifier.create(service.execute("SUB", "01", "A", "actor"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.AGENCY_NOT_FOUND, ((AppException) err).getError());
                })
                .verify();
    }
}

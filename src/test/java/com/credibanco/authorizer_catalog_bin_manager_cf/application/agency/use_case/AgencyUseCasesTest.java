package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgencyUseCasesTest {

    private AgencyRepository repo;
    private SubtypeReadOnlyRepository subtypeRepo;
    private TransactionalOperator tx;

    @BeforeEach
    void setUp() {
        repo = mock(AgencyRepository.class);
        subtypeRepo = mock(SubtypeReadOnlyRepository.class);
        tx = mock(TransactionalOperator.class);
        when(tx.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createAgencyValidatesSubtypeAndDuplicates() {
        Agency draft = Agency.createNew("SUB", "01", "Main", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, "user");

        when(subtypeRepo.existsByCode("SUB")).thenReturn(Mono.just(true));
        when(repo.existsByPk("SUB", "01")).thenReturn(Mono.just(true));

        CreateAgencyService service = new CreateAgencyService(repo, subtypeRepo, tx);

        StepVerifier.create(service.execute(draft))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.AGENCY_ALREADY_EXISTS, ((AppException) err).getError());
                })
                .verify();

        when(repo.existsByPk("SUB", "01")).thenReturn(Mono.just(false));
        when(repo.save(any())).thenReturn(Mono.just(draft));

        StepVerifier.create(service.execute(draft))
                .expectNext(draft)
                .verifyComplete();
    }

    @Test
    void createAgencyFailsWhenSubtypeMissing() {
        Agency draft = Agency.createNew("SUB", "01", "Main", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);

        when(subtypeRepo.existsByCode("SUB")).thenReturn(Mono.just(false));

        CreateAgencyService service = new CreateAgencyService(repo, subtypeRepo, tx);

        StepVerifier.create(service.execute(draft))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) err).getError());
                })
                .verify();
    }

    @Test
    void updateAgencyMergesExistingAggregate() {
        Agency existing = Agency.rehydrate("SUB", "01", "Main", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                "Old desc", "A", null, null, "old");
        Agency update = new Agency("SUB", "01", "Updated", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                "New desc", "A", null, null, "editor");

        when(subtypeRepo.existsByCode("SUB")).thenReturn(Mono.just(true));
        when(repo.findByPk("SUB", "01")).thenReturn(Mono.just(existing));
        when(repo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        UpdateAgencyService service = new UpdateAgencyService(repo, subtypeRepo, tx);

        StepVerifier.create(service.execute(update))
                .assertNext(saved -> {
                    assertEquals("Updated", saved.name());
                    assertEquals("New desc", saved.description());
                    assertEquals("editor", saved.updatedBy());
                })
                .verifyComplete();

        ArgumentCaptor<Agency> saved = ArgumentCaptor.forClass(Agency.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().createdAt()).isNull();
    }

    @Test
    void updateAgencyHandlesMissingResources() {
        Agency update = new Agency("SUB", "99", "Updated", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, "A", null, null, "editor");

        when(subtypeRepo.existsByCode("SUB")).thenReturn(Mono.just(true));
        when(repo.findByPk("SUB", "99")).thenReturn(Mono.empty());

        UpdateAgencyService service = new UpdateAgencyService(repo, subtypeRepo, tx);

        StepVerifier.create(service.execute(update))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.AGENCY_NOT_FOUND, ((AppException) err).getError());
                })
                .verify();
    }

    @Test
    void changeStatusProtectsLastActiveAgency() {
        Agency current = Agency.createNew("SUB", "01", "Main", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);

        when(repo.findByPk("SUB", "01")).thenReturn(Mono.just(current));
        when(subtypeRepo.existsByCode("SUB")).thenReturn(Mono.just(true));
        when(repo.existsAnotherActive("SUB", "01")).thenReturn(Mono.just(false));

        ChangeAgencyStatusService service = new ChangeAgencyStatusService(repo, subtypeRepo, tx);

        StepVerifier.create(service.execute("SUB", "01", "I", "actor"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.AGENCY_CONFLICT_RULE, ((AppException) err).getError());
                })
                .verify();
    }

    @Test
    void changeStatusRejectsInvalidStatus() {
        ChangeAgencyStatusService service = new ChangeAgencyStatusService(repo, subtypeRepo, tx);

        StepVerifier.create(service.execute("SUB", "01", "X", "actor"))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof AppException);
                    assertEquals(AppError.AGENCY_INVALID_DATA, ((AppException) err).getError());
                })
                .verify();
    }
}

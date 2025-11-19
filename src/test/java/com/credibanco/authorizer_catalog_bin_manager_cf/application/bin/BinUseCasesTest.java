package com.credibanco.authorizer_catalog_bin_manager_cf.application.bin;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BinUseCasesTest {

    private BinRepository repo;
    private TransactionalOperator tx;
    private SubtypeReadOnlyRepository subtypeRepo;

    @BeforeEach
    void setup() {
        repo = mock(BinRepository.class);
        subtypeRepo = mock(SubtypeReadOnlyRepository.class);
        tx = mock(TransactionalOperator.class);
        lenient().when(tx.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(tx.transactional(any(Flux.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createBinChecksExistenceThenSaves() {
        CreateBinService service = new CreateBinService(repo, tx);
        Bin expected = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, "actor");

        when(repo.existsById("123456")).thenReturn(Mono.just(false));
        when(repo.save(any(Bin.class))).thenReturn(Mono.just(expected));

        StepVerifier.create(service.execute("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, "actor"))
                .expectNext(expected)
                .verifyComplete();

        verify(repo).existsById("123456");
        verify(repo).save(any(Bin.class));
    }

    @Test
    void createBinFailsWhenAlreadyExists() {
        CreateBinService service = new CreateBinService(repo, tx);
        when(repo.existsById("123456")).thenReturn(Mono.just(true));

        StepVerifier.create(service.execute("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null))
                .expectErrorSatisfies(err -> {
                    assertEquals(AppException.class, err.getClass());
                    assertEquals(AppError.BIN_ALREADY_EXISTS, ((AppException) err).getError());
                })
                .verify();
    }

    @Test
    void getBinReturnsAggregateOrError() {
        Bin found = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null);
        GetBinService service = new GetBinService(repo);
        when(repo.findById("123456")).thenReturn(Mono.just(found));

        StepVerifier.create(service.execute("123456"))
                .expectNext(found)
                .verifyComplete();

        when(repo.findById("999999")).thenReturn(Mono.empty());
        StepVerifier.create(service.execute("999999"))
                .expectErrorSatisfies(err -> assertEquals(AppError.BIN_NOT_FOUND, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void listBinsValidatesParameters() {
        ListBinsService service = new ListBinsService(repo);
        when(repo.findAll(0, 1)).thenReturn(Flux.just(Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null)));

        StepVerifier.create(service.execute(0, 1).collectList())
                .expectNextMatches(list -> list.size() == 1)
                .verifyComplete();

        StepVerifier.create(service.execute(-1, 0))
                .expectErrorSatisfies(err -> assertEquals(AppError.BIN_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void updateBinValidatesSubtypeRestrictionAndSaves() {
        Bin current = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null);
        Bin updated = current.updateBasics("NEW", "DEBITO", "12", "CC", "DESC", "N", null, "actor");
        UpdateBinService service = new UpdateBinService(repo, subtypeRepo, tx);

        when(repo.findById("123456")).thenReturn(Mono.just(current));
        when(subtypeRepo.existsAnyByBin("123456")).thenReturn(Mono.just(false));
        when(repo.save(any(Bin.class))).thenReturn(Mono.just(updated));

        StepVerifier.create(service.execute("123456", "NEW", "DEBITO", "12", "CC", "DESC", "N", null, "actor"))
                .expectNext(updated)
                .verifyComplete();

        verify(subtypeRepo, never()).existsAnyByBin("123456");
    }

    @Test
    void updateBinRejectsChangeWhenSubtypeExists() {
        Bin current = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null);
        UpdateBinService service = new UpdateBinService(repo, subtypeRepo, tx);

        when(repo.findById("123456")).thenReturn(Mono.just(current));
        when(subtypeRepo.existsAnyByBin("123456")).thenReturn(Mono.just(true));

        StepVerifier.create(service.execute("123456", "NAME", "DEBITO", "12", "CC", "DESC", "Y", 1, null))
                .expectErrorSatisfies(err -> assertEquals(AppError.BIN_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void changeStatusValidatesNewStatusAndSaves() {
        Bin current = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null);
        ChangeBinStatusService service = new ChangeBinStatusService(repo, tx);

        when(repo.findById("123456")).thenReturn(Mono.just(current));
        when(repo.save(any(Bin.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.execute("123456", "I", "user"))
                .expectNextMatches(bin -> "I".equals(bin.status()) && "user".equals(bin.updatedBy()))
                .verifyComplete();

        StepVerifier.create(service.execute("123456", "X", "user"))
                .expectErrorSatisfies(err -> assertEquals(AppError.BIN_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }
}

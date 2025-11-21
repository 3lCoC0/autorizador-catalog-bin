package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.AgencyReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.IdTypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SubtypeUseCasesTest {

    private SubtypeRepository repo;
    private BinReadOnlyRepository binRepo;
    private IdTypeReadOnlyRepository idTypeRepo;
    private AgencyReadOnlyRepository agencyRepo;
    private TransactionalOperator tx;

    @BeforeEach
    void setup() {
        repo = mock(SubtypeRepository.class);
        binRepo = mock(BinReadOnlyRepository.class);
        idTypeRepo = mock(IdTypeReadOnlyRepository.class);
        agencyRepo = mock(AgencyReadOnlyRepository.class);
        tx = mock(TransactionalOperator.class);

        lenient().when(tx.transactional(ArgumentMatchers.<Mono<?>>any()))
                .thenAnswer(inv -> inv.getArgument(0));

        lenient().when(tx.transactional(ArgumentMatchers.<Flux<?>>any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createSubtypeValidatesUniquenessAndSaves() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 1)));
        when(binRepo.existsById(anyString())).thenReturn(Mono.just(false));
        when(idTypeRepo.existsById("CC")).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of("CC")));
        when(repo.existsByPk(anyString(), anyString())).thenReturn(Mono.just(false));
        when(repo.existsByBinAndExt(anyString(), anyString())).thenReturn(Mono.just(false));
        when(repo.existsBySubtypeCode(anyString())).thenReturn(Mono.just(false));
        when(repo.save(any(Subtype.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.execute("ABC", "123456", "NAME", "DESC", "CC", "123", "7", "creator"))
                .expectNextMatches(s -> "I".equals(s.status()) && "1234567".equals(s.binEfectivo()))
                .verifyComplete();

        verify(repo).existsByPk("123456", "ABC");
        verify(repo).existsByBinAndExt("123456", "7");
        verify(repo).existsBySubtypeCode("ABC");
        verify(repo).save(any(Subtype.class));
    }

    @Test
    void createSubtypeRejectsMissingBinConfig() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("999999")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("ABC", "999999", "NAME", "DESC", null, null, null, null))
                .expectErrorSatisfies(err -> assertEquals(AppError.BIN_NOT_FOUND, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void getSubtypeReturnsAggregateOrErrors() {
        GetSubtypeService service = new GetSubtypeService(repo);
        Subtype found = Subtype.createNew("ABC", "123456", "NAME", "DESC", null, null, null, null);
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(found));

        StepVerifier.create(service.execute("123456", "ABC"))
                .expectNext(found)
                .verifyComplete();

        when(repo.findByPk("000000", "ZZZ")).thenReturn(Mono.empty());
        StepVerifier.create(service.execute("000000", "ZZZ"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void listSubtypesValidatesPagingAndBinExistence() {
        ListSubtypesService service = new ListSubtypesService(repo, binRepo);
        when(binRepo.existsById("123456")).thenReturn(Mono.just(true));
        when(repo.findAll("123456", null, null, 0, 2)).thenReturn(Flux.just(
                Subtype.createNew("ABC", "123456", "NAME", "DESC", null, null, null, null)));

        StepVerifier.create(service.execute("123456", null, null, 0, 2).collectList())
                .expectNextMatches(list -> list.size() == 1)
                .verifyComplete();

        StepVerifier.create(service.execute("123456", null, null, -1, 0))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();

        when(binRepo.existsById("000000")).thenReturn(Mono.just(false));
        StepVerifier.create(service.execute("000000", null, null, 0, 1))
                .expectErrorSatisfies(err -> assertEquals(AppError.BIN_NOT_FOUND, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void updateSubtypeChecksFkAndExtensionChanges() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = Subtype.createNew("ABC", "123456", "NAME", "DESC", "CC", "123", null, null);
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 2)));
        when(idTypeRepo.existsById("CC")).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of("CC")));
        when(repo.existsByBinAndExt("123456", "05")).thenReturn(Mono.just(false));
        when(repo.save(any(Subtype.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.execute("123456", "ABC", "NEW", "DESC", "CC", "123", "5", "actor"))
                .expectNextMatches(s -> "NEW".equals(s.name()) && "12345605".equals(s.binEfectivo()))
                .verifyComplete();

        verify(repo).existsByBinAndExt("123456", "05");
    }

    @Test
    void updateSubtypeRejectsInvalidFk() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = Subtype.createNew("ABC", "123456", "NAME", "DESC", "CC", "123", null, null);
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("N", 0)));
        when(idTypeRepo.existsById("XX")).thenReturn(Mono.just(false));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of("CC", "TI")));

        StepVerifier.create(service.execute("123456", "ABC", "NEW", "DESC", "XX", "123", null, "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void changeStatusValidatesNewStatusAndAgencyRequirement() {
        ChangeSubtypeStatusService service = new ChangeSubtypeStatusService(repo, agencyRepo, tx);
        Subtype current = Subtype.createNew("ABC", "123456", "NAME", "DESC", null, null, null, null);
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(current));
        when(repo.save(any(Subtype.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        when(agencyRepo.countActiveBySubtypeCode("ABC")).thenReturn(Mono.just(0L));
        StepVerifier.create(service.execute("123456", "ABC", "A", "user"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_ACTIVATE_REQUIRES_AGENCY, ((AppException) err).getError()))
                .verify();

        when(agencyRepo.countActiveBySubtypeCode("ABC")).thenReturn(Mono.just(2L));
        StepVerifier.create(service.execute("123456", "ABC", "A", "user"))
                .expectNextMatches(s -> "A".equals(s.status()) && "user".equals(s.updatedBy()))
                .verifyComplete();

        StepVerifier.create(service.execute("123456", "ABC", "X", "user"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }
}

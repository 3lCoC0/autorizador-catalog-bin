package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.IdTypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case.UpdateSubtypeBasicsService;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UpdateSubtypeBasicsServiceTest {

    private SubtypeRepository repo;
    private BinReadOnlyRepository binRepo;
    private IdTypeReadOnlyRepository idTypeRepo;
    private TransactionalOperator tx;

    @BeforeEach
    void setup() {
        repo = mock(SubtypeRepository.class);
        binRepo = mock(BinReadOnlyRepository.class);
        idTypeRepo = mock(IdTypeReadOnlyRepository.class);
        tx = mock(TransactionalOperator.class);
        lenient().when(tx.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Subtype existingSubtypeWithExt(String ext) {
        return Subtype.createNew("ABC", "123456", "NAME", "DESC", "CC", "123", ext, null);
    }

    @Test
    void updatesWithoutChangingExtensionSkipsCollisionCheck() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = existingSubtypeWithExt(null);
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("N", 0)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));
        when(repo.save(any(Subtype.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.execute("123456", "ABC", "NEW", "DESC", "CC", "123", null, "actor"))
                .expectNextMatches(s -> "NEW".equals(s.name()) && s.binExt() == null)
                .verifyComplete();

        verify(repo, never()).existsByBinAndExt(anyString(), anyString());
    }

    @Test
    void rejectsMissingSubtype() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("123456", "ABC", "NEW", "DESC", null, null, null, "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_NOT_FOUND, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsMissingBinConfig() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = existingSubtypeWithExt(null);
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("123456", "ABC", "NEW", "DESC", null, null, null, "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.BIN_NOT_FOUND, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsInvalidOwnerTypeWithAvailableCatalog() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = existingSubtypeWithExt(null);
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("N", 0)));
        when(idTypeRepo.existsById("XX")).thenReturn(Mono.just(false));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of("CC", "TI")));

        StepVerifier.create(service.execute("123456", "ABC", "NEW", "DESC", "XX", "123", null, "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsInvalidOwnerTypeWithoutCatalog() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = existingSubtypeWithExt(null);
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("N", 0)));
        when(idTypeRepo.existsById("XX")).thenReturn(Mono.just(false));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));

        StepVerifier.create(service.execute("123456", "ABC", "NEW", "DESC", "XX", "123", null, "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsExtensionWhenNotAllowed() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = existingSubtypeWithExt(null);
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("N", 0)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));

        StepVerifier.create(service.execute("123456", "ABC", "NEW", "DESC", "CC", "123", "1", "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsMissingRequiredExtension() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = existingSubtypeWithExt(null);
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 2)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));

        StepVerifier.create(service.execute("123456", "ABC", "NEW", "DESC", "CC", "123", null, "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsExtensionTooLongForDigits() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = existingSubtypeWithExt(null);
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 1)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));

        StepVerifier.create(service.execute("123456", "ABC", "NEW", "DESC", "CC", "123", "22", "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsExtensionDigitsConfigOutOfRange() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = existingSubtypeWithExt(null);
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 0)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));

        StepVerifier.create(service.execute("123456", "ABC", "NEW", "DESC", "CC", "123", "1", "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsExtensionOverflowingBinLength() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = Subtype.createNew("ABC", "12345678", "NAME", "DESC", "CC", "123", null, null);
        when(repo.findByPk("12345678", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("12345678")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 2)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));

        StepVerifier.create(service.execute("12345678", "ABC", "NEW", "DESC", "CC", "123", "123", "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsBinAlreadyNineDigitsWithExtensionConfig() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = Subtype.createNew("ABC", "123456789", "NAME", "DESC", "CC", "123", null, null);
        when(repo.findByPk("123456789", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("123456789")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 1)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));

        StepVerifier.create(service.execute("123456789", "ABC", "NEW", "DESC", "CC", "123", "1", "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsInvalidUpdatedAggregate() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = existingSubtypeWithExt(null);
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("N", 0)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));

        StepVerifier.create(service.execute("123456", "ABC", " ", "DESC", "CC", "123", null, "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void detectsExtensionCollisionWhenChanged() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = existingSubtypeWithExt("1");
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 1)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));
        when(repo.existsByBinAndExt("123456", "2")).thenReturn(Mono.just(true));

        StepVerifier.create(service.execute("123456", "ABC", "NEW", "DESC", "CC", "123", "2", "actor"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_ALREADY_EXISTS, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void updatesWithExtensionChangePersistsAggregate() {
        UpdateSubtypeBasicsService service = new UpdateSubtypeBasicsService(repo, binRepo, idTypeRepo, tx);
        Subtype current = existingSubtypeWithExt("1");
        when(repo.findByPk("123456", "ABC")).thenReturn(Mono.just(current));
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 2)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));
        when(repo.existsByBinAndExt("123456", "05")).thenReturn(Mono.just(false));
        when(repo.save(any(Subtype.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.execute("123456", "ABC", "NEW", "DESC", "CC", "123", "5", "actor"))
                .expectNextMatches(s -> "12345605".equals(s.binEfectivo()))
                .verifyComplete();

        verify(repo).existsByBinAndExt("123456", "05");
    }
}

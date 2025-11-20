package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.IdTypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case.CreateSubtypeService;
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

class CreateSubtypeServiceTest {

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

    @Test
    void createsSubtypeFormattingExtensionAndSaving() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 3)));
        when(binRepo.existsById(anyString())).thenReturn(Mono.just(false));
        when(idTypeRepo.existsById("CC")).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of("CC")));
        when(repo.existsByPk("123456", "ABC")).thenReturn(Mono.just(false));
        when(repo.existsByBinAndExt("123456", "007")).thenReturn(Mono.just(false));
        when(repo.existsBySubtypeCode("ABC")).thenReturn(Mono.just(false));
        when(repo.save(any(Subtype.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.execute("ABC", "123456", "NAME", "DESC", "CC", "123", "7", "creator"))
                .expectNextMatches(s -> "123456007".equals(s.binEfectivo()))
                .verifyComplete();
    }

    @Test
    void rejectsMissingBinConfig() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("999999")).thenReturn(Mono.empty());

        StepVerifier.create(service.execute("ABC", "999999", "NAME", "DESC", null, null, null, null))
                .expectErrorSatisfies(err -> assertEquals(AppError.BIN_NOT_FOUND, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsUnknownIdTypeWithAvailableCatalog() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("N", 0)));
        when(idTypeRepo.existsById("XX")).thenReturn(Mono.just(false));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of("CC", "TI")));

        StepVerifier.create(service.execute("ABC", "123456", "NAME", "DESC", "XX", "123", null, "creator"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsUnknownIdTypeWithoutCatalog() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("N", 0)));
        when(idTypeRepo.existsById("XX")).thenReturn(Mono.just(false));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));

        StepVerifier.create(service.execute("ABC", "123456", "NAME", "DESC", "XX", "123", null, "creator"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsMissingRequiredExtension() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 2)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of("CC")));

        StepVerifier.create(service.execute("ABC", "123456", "NAME", "DESC", "CC", "123", null, "creator"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsExtensionWhenNotAllowed() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("N", 0)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));

        StepVerifier.create(service.execute("ABC", "123456", "NAME", "DESC", "CC", "123", "1", "creator"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsExtensionTooLongForDigits() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 1)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));

        StepVerifier.create(service.execute("ABC", "123456", "NAME", "DESC", "CC", "123", "22", "creator"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsExtensionDigitsConfigOutOfRange() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 0)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of("CC")));

        StepVerifier.create(service.execute("ABC", "123456", "NAME", "DESC", "CC", "123", "1", "creator"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsExtensionWhenBinAlreadyNineDigits() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("123456789")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 1)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of("CC")));

        StepVerifier.create(service.execute("ABC", "123456789", "NAME", "DESC", "CC", "123", "1", "creator"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsExtensionOverflowingBinLength() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("12345678")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 2)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of("CC")));

        StepVerifier.create(service.execute("ABC", "12345678", "NAME", "DESC", "CC", "123", "123", "creator"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void rejectsInvalidSubtypeDataFromAggregate() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("N", 0)));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of()));

        StepVerifier.create(service.execute("ABC", "123456", " ", "DESC", "CC", "123", null, "creator"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void detectsMasterBinCollision() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 1)));
        when(binRepo.existsById("1234567")).thenReturn(Mono.just(true));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of("CC")));
        when(repo.existsByPk(anyString(), anyString())).thenReturn(Mono.just(false));
        when(repo.existsByBinAndExt(anyString(), anyString())).thenReturn(Mono.just(false));
        when(repo.existsBySubtypeCode(anyString())).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute("ABC", "123456", "NAME", "DESC", "CC", "123", "7", "creator"))
                .expectErrorSatisfies(err -> assertEquals(AppError.BIN_ALREADY_EXISTS, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void detectsExistingSubtypeByPrimaryKey() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 1)));
        when(binRepo.existsById(anyString())).thenReturn(Mono.just(false));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of("CC")));
        when(repo.existsByPk("123456", "ABC")).thenReturn(Mono.just(true));
        when(repo.existsByBinAndExt(anyString(), anyString())).thenReturn(Mono.just(false));
        when(repo.existsBySubtypeCode(anyString())).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute("ABC", "123456", "NAME", "DESC", "CC", "123", "7", "creator"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_ALREADY_EXISTS, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void detectsExistingSubtypeByExtension() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 1)));
        when(binRepo.existsById(anyString())).thenReturn(Mono.just(false));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of("CC")));
        when(repo.existsByPk(anyString(), anyString())).thenReturn(Mono.just(false));
        when(repo.existsByBinAndExt("123456", "7")).thenReturn(Mono.just(true));
        when(repo.existsBySubtypeCode(anyString())).thenReturn(Mono.just(false));

        StepVerifier.create(service.execute("ABC", "123456", "NAME", "DESC", "CC", "123", "7", "creator"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_ALREADY_EXISTS, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void detectsExistingSubtypeByCode() {
        CreateSubtypeService service = new CreateSubtypeService(repo, binRepo, idTypeRepo, tx);
        when(binRepo.getExtConfig("123456")).thenReturn(Mono.just(new BinReadOnlyRepository.BinExtConfig("Y", 1)));
        when(binRepo.existsById(anyString())).thenReturn(Mono.just(false));
        when(idTypeRepo.existsById(anyString())).thenReturn(Mono.just(true));
        when(idTypeRepo.findAllCodes()).thenReturn(Mono.just(List.of("CC")));
        when(repo.existsByPk(anyString(), anyString())).thenReturn(Mono.just(false));
        when(repo.existsByBinAndExt(anyString(), anyString())).thenReturn(Mono.just(false));
        when(repo.existsBySubtypeCode("ABC")).thenReturn(Mono.just(true));

        StepVerifier.create(service.execute("ABC", "123456", "NAME", "DESC", "CC", "123", "7", "creator"))
                .expectErrorSatisfies(err -> assertEquals(AppError.SUBTYPE_ALREADY_EXISTS, ((AppException) err).getError()))
                .verify();
    }
}

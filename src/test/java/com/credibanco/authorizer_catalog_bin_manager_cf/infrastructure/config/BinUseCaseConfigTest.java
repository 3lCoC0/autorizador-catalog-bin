package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.SubtypeReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.use_case.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class BinUseCaseConfigTest {

    private BinUseCaseConfig config;
    private BinRepository binRepository;
    private SubtypeReadOnlyRepository subtypeRepository;
    private TransactionalOperator transactionalOperator;

    @BeforeEach
    void setUp() {
        config = new BinUseCaseConfig();
        binRepository = mock(BinRepository.class);
        subtypeRepository = mock(SubtypeReadOnlyRepository.class);
        transactionalOperator = mock(TransactionalOperator.class);
    }

    @Test
    void getBinUseCaseCreatesServiceWithRepository() {
        GetBinUseCase useCase = config.getBinUseCase(binRepository);
        assertTrue(useCase instanceof GetBinService);
        GetBinService service = (GetBinService) useCase;
        assertSame(binRepository, extractField(service, "repo"));
    }

    @Test
    void listBinsUseCaseCreatesServiceWithRepository() {
        ListBinsUseCase useCase = config.listBinsUseCase(binRepository);
        assertTrue(useCase instanceof ListBinsService);
        ListBinsService service = (ListBinsService) useCase;
        assertSame(binRepository, extractField(service, "repo"));
    }

    @Test
    void createBinUseCaseCreatesServiceWithDependencies() {
        CreateBinUseCase useCase = config.createBinUseCase(binRepository, transactionalOperator);
        assertTrue(useCase instanceof CreateBinService);
        CreateBinService service = (CreateBinService) useCase;
        assertSame(binRepository, service.repo());
        assertSame(transactionalOperator, service.tx());
    }

    @Test
    void changeBinStatusUseCaseCreatesServiceWithDependencies() {
        ChangeBinStatusUseCase useCase = config.changeBinStatusUseCase(binRepository, transactionalOperator);
        assertTrue(useCase instanceof ChangeBinStatusService);
        ChangeBinStatusService service = (ChangeBinStatusService) useCase;
        assertSame(binRepository, service.repo());
        assertSame(transactionalOperator, service.tx());
    }

    @Test
    void updateBinUseCaseCreatesServiceWithDependencies() {
        UpdateBinUseCase useCase = config.updateBinUseCase(binRepository, subtypeRepository, transactionalOperator);
        assertTrue(useCase instanceof UpdateBinService);
        UpdateBinService service = (UpdateBinService) useCase;
        assertSame(binRepository, service.repo());
        assertSame(subtypeRepository, service.subtypeRepo());
        assertSame(transactionalOperator, service.tx());
    }

    private Object extractField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to read field " + fieldName, e);
        }
    }
}

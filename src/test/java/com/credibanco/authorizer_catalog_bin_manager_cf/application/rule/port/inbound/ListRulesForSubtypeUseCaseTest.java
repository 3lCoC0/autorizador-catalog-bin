package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;

class ListRulesForSubtypeUseCaseTest {

    private static class RecordingUseCase implements ListRulesForSubtypeUseCase {
        String subtypeCode;
        String bin;
        String status;
        Integer page;
        Integer size;

        @Override
        public Flux<ValidationMap> execute(String subtypeCode, String bin, String status, int page, int size) {
            this.subtypeCode = subtypeCode;
            this.bin = bin;
            this.status = status;
            this.page = page;
            this.size = size;
            return Flux.empty();
        }
    }

    @Test
    void defaultOverloadUsesActiveStatusWithDefaults() {
        RecordingUseCase useCase = new RecordingUseCase();

        useCase.execute("SUB", "654321").subscribe();

        assertEquals("SUB", useCase.subtypeCode);
        assertEquals("654321", useCase.bin);
        assertEquals("A", useCase.status);
        assertEquals(0, useCase.page);
        assertEquals(100, useCase.size);
    }

    @Test
    void overloadWithoutBinDelegatesWithNullBin() {
        RecordingUseCase useCase = new RecordingUseCase();

        useCase.execute("SUB2", "I", 2, 5).subscribe();

        assertEquals("SUB2", useCase.subtypeCode);
        assertNull(useCase.bin);
        assertEquals("I", useCase.status);
        assertEquals(2, useCase.page);
        assertEquals(5, useCase.size);
    }
}

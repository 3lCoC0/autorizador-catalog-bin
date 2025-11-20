package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;

class ListValidationsUseCaseTest {

    private static class RecordingUseCase implements ListValidationsUseCase {
        String status;
        String search;
        Integer page;
        Integer size;

        @Override
        public Flux<Validation> execute(String status, String search, int page, int size) {
            this.status = status;
            this.search = search;
            this.page = page;
            this.size = size;
            return Flux.empty();
        }
    }

    @Test
    void defaultExecutionUsesPaginationDefaults() {
        RecordingUseCase useCase = new RecordingUseCase();

        useCase.execute().subscribe();

        assertNull(useCase.status);
        assertNull(useCase.search);
        assertEquals(0, useCase.page);
        assertEquals(20, useCase.size);
    }
}

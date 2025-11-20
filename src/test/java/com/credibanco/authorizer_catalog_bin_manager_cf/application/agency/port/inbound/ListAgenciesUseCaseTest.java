package com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;

class ListAgenciesUseCaseTest {

    private static class RecordingUseCase implements ListAgenciesUseCase {
        String subtypeCode;
        String status;
        String search;
        Integer page;
        Integer size;

        @Override
        public Flux<Agency> execute(String subtypeCode, String status, String search, int page, int size) {
            this.subtypeCode = subtypeCode;
            this.status = status;
            this.search = search;
            this.page = page;
            this.size = size;
            return Flux.empty();
        }
    }

    @Test
    void defaultExecuteUsesStandardPagination() {
        RecordingUseCase useCase = new RecordingUseCase();

        useCase.execute("SUB", "A", "search-term").subscribe();

        assertEquals("SUB", useCase.subtypeCode);
        assertEquals("A", useCase.status);
        assertEquals("search-term", useCase.search);
        assertEquals(0, useCase.page);
        assertEquals(20, useCase.size);
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ChangePlanItemStatusUseCaseTest {

    private static class RecordingUseCase implements ChangePlanItemStatusUseCase {
        String planCode;
        String value;
        String status;
        String updatedBy;

        @Override
        public Mono<PlanItem> execute(String planCode, String value, String status, String updatedBy) {
            this.planCode = planCode;
            this.value = value;
            this.status = status;
            this.updatedBy = updatedBy;
            return Mono.just(PlanItem.rehydrate(1L, 2L, value, OffsetDateTime.now(), OffsetDateTime.now(), updatedBy, status));
        }
    }

    @Test
    void inactivateDelegatesWithInactiveStatus() {
        RecordingUseCase useCase = new RecordingUseCase();

        PlanItem item = useCase.inactivate("P1", "VAL1", "actor").block();

        assertEquals("P1", useCase.planCode);
        assertEquals("VAL1", useCase.value);
        assertEquals("I", useCase.status);
        assertEquals("actor", useCase.updatedBy);
        assertNotNull(item);
        assertEquals("I", item.status());
    }

    @Test
    void activateDelegatesWithActiveStatus() {
        RecordingUseCase useCase = new RecordingUseCase();

        PlanItem item = useCase.activate("P2", "VAL2", "actor2").block();

        assertEquals("P2", useCase.planCode);
        assertEquals("VAL2", useCase.value);
        assertEquals("A", useCase.status);
        assertEquals("actor2", useCase.updatedBy);
        assertNotNull(item);
        assertEquals("A", item.status());
    }
}

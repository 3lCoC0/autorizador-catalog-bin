package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ListRulesForSubtypeServiceTest {

    private final ValidationMapRepository mapRepository = mock(ValidationMapRepository.class);

    @Test
    void rejectsInvalidPaginationParameters() {
        ListRulesForSubtypeService service = new ListRulesForSubtypeService(mapRepository);

        StepVerifier.create(service.execute("ST", "123456", "A", -1, 0))
                .expectErrorSatisfies(err -> assertEquals(AppError.RULES_MAP_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void trimsBlankStatusBeforeDelegating() {
        ListRulesForSubtypeService service = new ListRulesForSubtypeService(mapRepository);
        ValidationMap map = ValidationMap.createNew("ST", "123456", 1L, "SI", null, null, "actor");
        when(mapRepository.findResolved(eq("ST"), eq("123456"), any(), eq(1), eq(10))).thenReturn(Flux.just(map));

        StepVerifier.create(service.execute("ST", "123456", "   ", 1, 10))
                .expectNext(map)
                .verifyComplete();

        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        verify(mapRepository).findResolved(eq("ST"), eq("123456"), statusCaptor.capture(), eq(1), eq(10));
        assertNull(statusCaptor.getValue());
    }

    @Test
    void forwardsStatusWhenProvided() {
        ListRulesForSubtypeService service = new ListRulesForSubtypeService(mapRepository);
        when(mapRepository.findResolved(anyString(), anyString(), anyString(), anyInt(), anyInt())).thenReturn(Flux.empty());

        StepVerifier.create(service.execute("ST", "123456", "A", 0, 5))
                .verifyComplete();

        verify(mapRepository).findResolved("ST", "123456", "A", 0, 5);
    }
}

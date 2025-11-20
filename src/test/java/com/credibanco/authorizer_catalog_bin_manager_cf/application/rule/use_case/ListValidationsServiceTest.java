package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListValidationsServiceTest {

    private final ValidationRepository repository = mock(ValidationRepository.class);

    @Test
    void rejectsInvalidPaginationParameters() {
        ListValidationsService service = new ListValidationsService(repository);

        StepVerifier.create(service.execute("A", null, -1, 0))
                .expectErrorSatisfies(err -> assertEquals(AppError.RULES_VALIDATION_INVALID_DATA, ((AppException) err).getError()))
                .verify();
    }

    @Test
    void delegatesToRepositoryWhenParametersValid() {
        ListValidationsService service = new ListValidationsService(repository);
        Validation validation = Validation.createNew("CODE", "DESC", ValidationDataType.TEXT, "actor");
        when(repository.findAll("A", "term", 0, 5)).thenReturn(Flux.just(validation));

        StepVerifier.create(service.execute("A", "term", 0, 5))
                .expectNext(validation)
                .verifyComplete();

        verify(repository).findAll("A", "term", 0, 5);
    }
}

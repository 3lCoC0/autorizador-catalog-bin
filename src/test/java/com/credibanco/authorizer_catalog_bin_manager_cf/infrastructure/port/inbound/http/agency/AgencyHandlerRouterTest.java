package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.http.ApiSuccess;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security.ActorProvider;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto.AgencyCreateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto.AgencyStatusRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.handler.AgencyHandler;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.router.AgencyRouter;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.Map;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgencyHandlerRouterTest {

    private CreateAgencyUseCase createUC;
    private UpdateAgencyUseCase updateUC;
    private ChangeAgencyStatusUseCase changeStatusUC;
    private GetAgencyUseCase getUC;
    private ListAgenciesUseCase listUC;
    private ActorProvider actorProvider;
    private ValidationUtil validationUtil;

    @BeforeEach
    void setUp() {
        createUC = mock(CreateAgencyUseCase.class);
        updateUC = mock(UpdateAgencyUseCase.class);
        changeStatusUC = mock(ChangeAgencyStatusUseCase.class);
        getUC = mock(GetAgencyUseCase.class);
        listUC = mock(ListAgenciesUseCase.class);
        actorProvider = mock(ActorProvider.class);
        validationUtil = new ValidationUtil(Validation.buildDefaultValidatorFactory().getValidator());

        Agency sample = Agency.rehydrate("SUB", "00", "ANY", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, "A", OffsetDateTime.now(), OffsetDateTime.now(), null);
        when(listUC.execute(anyString(), any(), any(), anyInt(), anyInt())).thenReturn(Flux.just(sample));
        when(getUC.execute(anyString(), anyString())).thenReturn(Mono.just(sample));
    }

    @Test
    void routerExposesAllAgencyEndpoints() {
        AgencyHandler handler = new AgencyHandler(createUC, updateUC, changeStatusUC, getUC, listUC, validationUtil, actorProvider);
        AgencyRouter router = new AgencyRouter(handler);
        WebTestClient client = WebTestClient.bindToRouterFunction(routerFunction(router)).build();

        client.get().uri("/agencies/list/ABC?subtypeCode=ABC").accept(MediaType.APPLICATION_JSON)
                .exchange().expectStatus().isOk();
        client.get().uri("/agencies/get/ABC/01").accept(MediaType.APPLICATION_JSON)
                .exchange().expectStatus().isOk();
    }

    @Test
    void createEndpointNormalizesAndDelegates() {
        AgencyHandler handler = new AgencyHandler(createUC, updateUC, changeStatusUC, getUC, listUC, validationUtil, actorProvider);
        AgencyRouter router = new AgencyRouter(handler);
        WebTestClient client = WebTestClient.bindToRouterFunction(routerFunction(router)).build();

        Agency created = Agency.rehydrate("SUB", "01", "MAIN", "123", null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                "Desc", "A", OffsetDateTime.now(), OffsetDateTime.now(), "ACTOR");

        when(actorProvider.currentUserId()).thenReturn(Mono.just("actor"));
        when(createUC.execute(any())).thenReturn(Mono.just(created));

        AgencyCreateRequest request = new AgencyCreateRequest(
                "sub", "01", "main", "123",
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                "desc", null
        );

        ApiSuccess<?> response = client.post().uri("/agencies/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", ".*/agencies/create/SUB/01")
                .expectBody(ApiSuccess.class)
                .returnResult().getResponseBody();

        assertNotNull(response);
        ArgumentCaptor<Agency> agencyCaptor = ArgumentCaptor.forClass(Agency.class);
        verify(createUC).execute(agencyCaptor.capture());
        assertThat(agencyCaptor.getValue().subtypeCode()).isEqualTo("SUB");
        assertThat(agencyCaptor.getValue().updatedBy()).isEqualTo("actor");
        assertEquals("SUB", ((Map<?, ?>) response.data()).get("subtypeCode"));
    }

    @Test
    void changeStatusValidatesPayload() {
        AgencyHandler handler = new AgencyHandler(createUC, updateUC, changeStatusUC, getUC, listUC, validationUtil, actorProvider);
        AgencyStatusRequest invalid = new AgencyStatusRequest("X", "user");
        MockServerRequest req = MockServerRequest.builder()
                .pathVariable("subtypeCode", "SUB")
                .pathVariable("agencyCode", "01")
                .body(Mono.just(invalid));

        StepVerifier.create(handler.changeStatus(req))
                .expectErrorSatisfies(err -> assertTrue(err instanceof AppException))
                .verify();
    }

    @SuppressWarnings("unchecked")
    private RouterFunction<ServerResponse> routerFunction(AgencyRouter router) {
        try {
            Method routes = AgencyRouter.class.getDeclaredMethod("routes");
            routes.setAccessible(true);
            return (RouterFunction<ServerResponse>) routes.invoke(router);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to invoke router", e);
        }
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security.ActorProvider;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto.SubtypeCreateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto.SubtypeStatusRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto.SubtypeUpdateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.router.SubtypeRouter;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubtypeHandlerTest {

    private CreateSubtypeUseCase createUC;
    private ListSubtypesUseCase listUC;
    private UpdateSubtypeBasicsUseCase updateUC;
    private GetSubtypeUseCase getUC;
    private ChangeSubtypeStatusUseCase changeStatusUC;
    private ValidationUtil validation;
    private ActorProvider actorProvider;
    private WebTestClient client;

    @BeforeEach
    void setup() {
        createUC = mock(CreateSubtypeUseCase.class);
        listUC = mock(ListSubtypesUseCase.class);
        updateUC = mock(UpdateSubtypeBasicsUseCase.class);
        getUC = mock(GetSubtypeUseCase.class);
        changeStatusUC = mock(ChangeSubtypeStatusUseCase.class);
        validation = mock(ValidationUtil.class);
        actorProvider = mock(ActorProvider.class);

        SubtypeHandler handler = new SubtypeHandler(createUC, listUC, validation, updateUC, getUC, changeStatusUC, actorProvider);
        SubtypeRouter router = new SubtypeRouter(handler);
        try {
            var method = SubtypeRouter.class.getDeclaredMethod("subtypeRoutes");
            method.setAccessible(true);
            client = WebTestClient.bindToRouterFunction((RouterFunction<ServerResponse>) method.invoke(router))
                    .configureClient().baseUrl("/").build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(validation.validate(any(), any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(actorProvider.currentUserId()).thenReturn(Mono.just("secUser"));
    }

    @Test
    void createEndpointReturnsCreatedEnvelope() {
        Subtype created = Subtype.createNew("ABC", "123456", "NAME", "DESC", "CC", "123", "7", "creator");
        when(createUC.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(created));

        SubtypeCreateRequest request = new SubtypeCreateRequest("ABC", "123456", "NAME", "DESC", "CC", "123", "7", "creator");

        client.post().uri("/subtypes/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.subtypeCode").isEqualTo("ABC");
    }

    @Test
    void listEndpointValidatesBinPath() {
        client.get().uri("/subtypes/list/bin/abcd")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void updateEndpointPassesPayload() {
        Subtype updated = Subtype.createNew("ABC", "123456", "NAME", "DESC", "CC", "123", null, null);
        when(updateUC.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(updated));

        SubtypeUpdateRequest request = new SubtypeUpdateRequest("NAME", "DESC", "CC", "123", "5", "actor");

        client.put().uri("/subtypes/update/123456/ABC")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.bin").isEqualTo("123456");
    }

    @Test
    void getEndpointReturnsSubtype() {
        Subtype aggregate = Subtype.createNew("ABC", "123456", "NAME", "DESC", null, null, null, null);
        when(getUC.execute("123456", "ABC")).thenReturn(Mono.just(aggregate));

        client.get().uri("/subtypes/get/123456/ABC")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.subtypeCode").isEqualTo("ABC");
    }

    @Test
    void changeStatusEndpointValidatesBody() {
        Subtype aggregate = Subtype.createNew("ABC", "123456", "NAME", "DESC", null, null, null, null);
        when(changeStatusUC.execute(eq("123456"), eq("ABC"), eq("I"), any()))
                .thenReturn(Mono.just(aggregate.changeStatus("I", "u")));

        SubtypeStatusRequest request = new SubtypeStatusRequest("I", "actor");

        client.put().uri("/subtypes/update/status/123456/ABC")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("I");
    }
}

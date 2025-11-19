package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.router;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.handler.PlanHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanRouterTest {

    @Mock
    private PlanHandler handler;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        PlanRouter router = new PlanRouter(handler);
        RouterFunction<ServerResponse> routes = router.routes();
        webTestClient = WebTestClient.bindToRouterFunction(routes).build();
    }

    @Test
    void postCreateRoutesToHandler() {
        when(handler.create(any(ServerRequest.class))).thenReturn(ServerResponse.ok().build());

        webTestClient.post().uri("/plans/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        verify(handler).create(any(ServerRequest.class));
    }

    @Test
    void getListRoutesToHandler() {
        when(handler.list(any(ServerRequest.class))).thenReturn(ServerResponse.ok().build());

        webTestClient.get().uri("/plans/list")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();

        verify(handler).list(any(ServerRequest.class));
    }

    @Test
    void getPlanRoutesToHandler() {
        when(handler.get(any(ServerRequest.class))).thenReturn(ServerResponse.ok().build());

        webTestClient.get().uri("/plans/get/CODE")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();

        verify(handler).get(any(ServerRequest.class));
    }

    @Test
    void putUpdateRoutesToHandler() {
        when(handler.update(any(ServerRequest.class))).thenReturn(ServerResponse.ok().build());

        webTestClient.put().uri("/plans/update/CODE")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        verify(handler).update(any(ServerRequest.class));
    }

    @Test
    void changeStatusRoutesToHandler() {
        when(handler.changeStatus(any(ServerRequest.class))).thenReturn(ServerResponse.ok().build());

        webTestClient.put().uri("/plans/update/status/CODE")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        verify(handler).changeStatus(any(ServerRequest.class));
    }

    @Test
    void itemAndAssignmentRoutesReachHandler() {
        when(handler.addItem(any(ServerRequest.class))).thenReturn(ServerResponse.ok().build());
        when(handler.listItems(any(ServerRequest.class))).thenReturn(ServerResponse.ok().build());
        when(handler.changeItemStatus(any(ServerRequest.class))).thenReturn(ServerResponse.ok().build());
        when(handler.assignToSubtype(any(ServerRequest.class))).thenReturn(ServerResponse.ok().build());

        webTestClient.post().uri("/plans/items/attach")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();
        verify(handler).addItem(any(ServerRequest.class));

        webTestClient.get().uri("/plans/items/get/CODE")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
        verify(handler).listItems(any(ServerRequest.class));

        webTestClient.put().uri("/plans/items/update/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();
        verify(handler).changeItemStatus(any(ServerRequest.class));

        webTestClient.post().uri("/plans/assign/subtype")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();
        verify(handler).assignToSubtype(any(ServerRequest.class));
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.router;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.handler.SubtypeHandler;
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
class SubtypeRouterTest {

    @Mock
    private SubtypeHandler handler;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        SubtypeRouter router = new SubtypeRouter(handler);
        RouterFunction<ServerResponse> routes = router.subtypeRoutes();
        webTestClient = WebTestClient.bindToRouterFunction(routes).build();
    }

    @Test
    void postCreateRoutesToHandler() {
        when(handler.create(any(ServerRequest.class)))
                .thenReturn(ServerResponse.ok().build());

        webTestClient.post()
                .uri("/subtypes/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        verify(handler).create(any(ServerRequest.class));
    }

    @Test
    void getListRoutesToHandler() {
        when(handler.listByBin(any(ServerRequest.class)))
                .thenReturn(ServerResponse.ok().build());

        webTestClient.get()
                .uri("/subtypes/list/bin/123456")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();

        verify(handler).listByBin(any(ServerRequest.class));
    }

    @Test
    void getByPkRoutesToHandler() {
        when(handler.get(any(ServerRequest.class)))
                .thenReturn(ServerResponse.ok().build());

        webTestClient.get()
                .uri("/subtypes/get/123456/ABC")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();

        verify(handler).get(any(ServerRequest.class));
    }

    @Test
    void putUpdateRoutesToHandler() {
        when(handler.update(any(ServerRequest.class)))
                .thenReturn(ServerResponse.ok().build());

        webTestClient.put()
                .uri("/subtypes/update/123456/ABC")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        verify(handler).update(any(ServerRequest.class));
    }

    @Test
    void putChangeStatusRoutesToHandler() {
        when(handler.changeStatus(any(ServerRequest.class)))
                .thenReturn(ServerResponse.ok().build());

        webTestClient.put()
                .uri("/subtypes/update/status/123456/ABC")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        verify(handler).changeStatus(any(ServerRequest.class));
    }
}

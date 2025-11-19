package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.router;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.handler.BinHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BinRouterTest {

    @Mock
    private BinHandler handler;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        BinRouter router = new BinRouter(handler);
        RouterFunction<ServerResponse> routes = router.routes();
        webTestClient = WebTestClient.bindToRouterFunction(routes).build();
    }

    @Test
    void postCreateRoutesToHandler() {
        when(handler.create(any(ServerRequest.class)))
                .thenReturn(Mono.just(ServerResponse.ok().build()));

        webTestClient.post()
                .uri("/bins/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        verify(handler).create(any(ServerRequest.class));
    }

    @Test
    void getListRoutesToHandler() {
        when(handler.list(any(ServerRequest.class)))
                .thenReturn(Mono.just(ServerResponse.ok().build()));

        webTestClient.get()
                .uri("/bins/list")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();

        verify(handler).list(any(ServerRequest.class));
    }

    @Test
    void getByBinRoutesToHandler() {
        when(handler.get(any(ServerRequest.class)))
                .thenReturn(Mono.just(ServerResponse.ok().build()));

        webTestClient.get()
                .uri("/bins/get/123456")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();

        verify(handler).get(any(ServerRequest.class));
    }

    @Test
    void putUpdateRoutesToHandler() {
        when(handler.update(any(ServerRequest.class)))
                .thenReturn(Mono.just(ServerResponse.ok().build()));

        webTestClient.put()
                .uri("/bins/update")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        verify(handler).update(any(ServerRequest.class));
    }

    @Test
    void putChangeStatusRoutesToHandler() {
        when(handler.changeStatus(any(ServerRequest.class)))
                .thenReturn(Mono.just(ServerResponse.ok().build()));

        webTestClient.put()
                .uri("/bins/update/status/123456")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        verify(handler).changeStatus(any(ServerRequest.class));
    }
}

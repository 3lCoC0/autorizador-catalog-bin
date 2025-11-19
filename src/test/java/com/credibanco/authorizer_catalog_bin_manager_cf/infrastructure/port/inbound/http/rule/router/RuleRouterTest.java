package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.router;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.handler.RuleHandler;
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
class RuleRouterTest {

    @Mock
    private RuleHandler handler;

    private WebTestClient client;

    @BeforeEach
    void setup() {
        RuleRouter router = new RuleRouter(handler);
        RouterFunction<ServerResponse> routes = router.routes();
        client = WebTestClient.bindToRouterFunction(routes).build();
    }

    @Test
    void createValidationRouteDelegates() {
        when(handler.createValidation(any(ServerRequest.class))).thenReturn(ServerResponse.ok().build());

        client.post().uri("/validations/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        verify(handler).createValidation(any(ServerRequest.class));
    }

    @Test
    void attachRuleRouteDelegates() {
        when(handler.attachRule(any(ServerRequest.class))).thenReturn(ServerResponse.ok().build());

        client.post().uri("/validations/attach")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        verify(handler).attachRule(any(ServerRequest.class));
    }

    @Test
    void listRulesRouteDelegates() {
        when(handler.listRulesForSubtype(any(ServerRequest.class))).thenReturn(ServerResponse.ok().build());

        client.get().uri("/v1/subtypes/ST/bins/123456/rules")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();

        verify(handler).listRulesForSubtype(any(ServerRequest.class));
    }
}

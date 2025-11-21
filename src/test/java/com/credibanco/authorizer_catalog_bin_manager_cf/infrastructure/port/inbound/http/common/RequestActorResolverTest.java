package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.common;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security.ActorProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestActorResolverTest {

    private ActorProvider actorProvider;
    private RequestActorResolver resolver;

    @BeforeEach
    void setUp() {
        actorProvider = mock(ActorProvider.class);
        when(actorProvider.currentUserId()).thenReturn(Mono.empty());
        resolver = new RequestActorResolver(actorProvider);
    }

    @Test
    void prefersActorFromBody() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/test").build());
        ServerRequest request = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());
        RequestActorResolver.ActorResolution resolution = resolver.resolve(request, "  bodyUser  ", "op")
                .block();

        Assertions.assertNotNull(resolution);
        assertThat(resolution.actor()).isEqualTo("bodyUser");
        assertThat(resolution.source()).isEqualTo(RequestActorResolver.ActorSource.REQUEST_BODY);
        assertThat(resolution.printableActor()).isEqualTo("bodyUser");
        assertThat(resolution.actorOrNull()).isEqualTo("bodyUser");
    }

    @Test
    void usesHeaderWhenBodyMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").header("X-User", " headerUser ").build());
        ServerRequest request = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());

        RequestActorResolver.ActorResolution resolution = resolver.resolve(request, "op")
                .block();

        Assertions.assertNotNull(resolution);
        assertThat(resolution.actor()).isEqualTo("headerUser");
        assertThat(resolution.source()).isEqualTo(RequestActorResolver.ActorSource.HEADER);
    }

    @Test
    void fallsBackToSecurityContext() {
        when(actorProvider.currentUserId()).thenReturn(Mono.just(" secUser "));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());
        ServerRequest request = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());

        RequestActorResolver.ActorResolution resolution = resolver.resolve(request, "op")
                .block();

        Assertions.assertNotNull(resolution);
        assertThat(resolution.actor()).isEqualTo("secUser");
        assertThat(resolution.source()).isEqualTo(RequestActorResolver.ActorSource.SECURITY_CONTEXT);
    }

    @Test
    void returnsNoneWhenNoActor() {
        when(actorProvider.currentUserId()).thenReturn(Mono.just("   "));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());
        ServerRequest request = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());

        RequestActorResolver.ActorResolution resolution = resolver.resolve(request, null, "op")
                .block();

        Assertions.assertNotNull(resolution);
        assertThat(resolution.actorOrNull()).isNull();
        assertThat(resolution.printableActor()).isEqualTo("<none>");
        assertThat(resolution.source()).isEqualTo(RequestActorResolver.ActorSource.NONE);
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.logging;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.ContextView;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationWebFilterTest {

    @Test
    void preservesIncomingCorrelationAndUser() {
        CorrelationWebFilter filter = new CorrelationWebFilter();
        MockServerHttpRequest request = MockServerHttpRequest.get("/path")
                .header(CorrelationWebFilter.CID, "cid-123")
                .header("X-User", "actor")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ContextView> contextRef = new AtomicReference<>();

        WebFilterChain chain = ex -> Mono.deferContextual(ctx -> {
            contextRef.set(ctx);
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationWebFilter.CID)).isEqualTo("cid-123");
        String contextCid = contextRef.get().get(CorrelationWebFilter.CTX_CID);
        String contextUser = contextRef.get().get(CorrelationWebFilter.CTX_USER);
        assertThat(contextCid).isEqualTo("cid-123");
        assertThat(contextUser).isEqualTo("actor");
    }

    @Test
    void generatesCorrelationIdWhenMissingOrPlaceholder() {
        CorrelationWebFilter filter = new CorrelationWebFilter();
        MockServerHttpRequest request = MockServerHttpRequest.get("/path")
                .header(CorrelationWebFilter.CID, "demo-cid-001")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ContextView> contextRef = new AtomicReference<>();

        WebFilterChain chain = ex -> Mono.deferContextual(ctx -> {
            contextRef.set(ctx);
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        String responseCid = exchange.getResponse().getHeaders().getFirst(CorrelationWebFilter.CID);
        assertThat(responseCid).isNotBlank();
        assertThat(responseCid).isNotEqualTo("demo-cid-001");
        String contextCid = contextRef.get().get(CorrelationWebFilter.CTX_CID);
        assertThat(contextCid).isEqualTo(responseCid);
        assertThat(contextRef.get().hasKey(CorrelationWebFilter.CTX_USER)).isFalse();
    }
}

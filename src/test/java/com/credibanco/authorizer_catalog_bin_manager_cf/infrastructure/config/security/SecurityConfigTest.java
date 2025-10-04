package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.TestConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@WebFluxTest
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://example.com",
        "internal.jwt.expected-issuer=test-issuer",
        "internal.jwt.required-audience=test-audience"
})
@Import({SecurityConfigTest.TestSecurityConfig.class, SecurityConfigTest.TestRoutes.class})
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void authorizationHeaderAuthenticatesRequest() {
        webTestClient.get()
                .uri("/secure-endpoint")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-from-authorization")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).isEqualTo("secured"));
    }

    @TestConfiguration
    static class TestSecurityConfig extends SecurityConfig {

        @Bean
        @Override
        public ReactiveJwtDecoder jwtDecoder() {
            return token -> Mono.just(new Jwt(
                    token,
                    Instant.now(),
                    Instant.now().plusSeconds(60),
                    Map.of("alg", "none"),
                    Map.of(
                            "roles", List.of("TEST"),
                            "iss", "test-issuer",
                            "aud", List.of("test-audience")
                    )
            ));
        }
    }

    @TestConfiguration
    static class TestRoutes {

        @Bean
        RouterFunction<ServerResponse> secureRoute() {
            return RouterFunctions.route()
                    .GET("/secure-endpoint", request -> request.principal()
                            .map(principal -> {
                                assertThat(principal).isInstanceOf(BearerTokenAuthenticationToken.class);
                                return (BearerTokenAuthenticationToken) principal;
                            })
                            .flatMap(authentication -> ServerResponse.ok().bodyValue("secured")))
                    .build();
        }
    }
}

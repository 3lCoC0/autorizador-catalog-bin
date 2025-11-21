package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "jwkSetUri", "http://example.com/jwks.json");
        ReflectionTestUtils.setField(securityConfig, "expectedIssuer", "expected-issuer");
        ReflectionTestUtils.setField(securityConfig, "requiredAudience", "expected-aud");
        ReflectionTestUtils.setField(securityConfig, "jwkTrustedCertificatePath", "");
    }

    @Test
    void xAuthTokenConverterPrefersHeader() {
        ServerAuthenticationConverter converter = securityConfig.xAuthTokenConverter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").header("X-Auth-Token", "abc").build());

        StepVerifier.create(converter.convert(exchange))
                .expectNextMatches(token -> token instanceof BearerTokenAuthenticationToken
                        && ((BearerTokenAuthenticationToken) token).getToken().equals("abc"))
                .verifyComplete();
    }

    @Test
    void xAuthTokenConverterReturnsEmptyWhenHeaderMissing() {
        ServerAuthenticationConverter converter = securityConfig.xAuthTokenConverter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());

        StepVerifier.create(converter.convert(exchange))
                .verifyComplete();
    }

    @Test
    void reactiveAuthenticationManagerMapsRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("roles", List.of("admin"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        ReactiveJwtDecoder decoder = token -> Mono.just(jwt);

        var manager = securityConfig.reactiveAuthenticationManager(decoder);
        BearerTokenAuthenticationToken authenticationToken = new BearerTokenAuthenticationToken("token");

        StepVerifier.create(manager.authenticate(authenticationToken))
                .expectNextMatches(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_admin")))
                .verifyComplete();
    }

    @Test
    void reactiveAuthenticationManagerUsesRealmAccessWhenRolesMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", java.util.Map.of("roles", List.of("user")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        ReactiveJwtDecoder decoder = token -> Mono.just(jwt);

        var manager = securityConfig.reactiveAuthenticationManager(decoder);
        BearerTokenAuthenticationToken authenticationToken = new BearerTokenAuthenticationToken("token");

        StepVerifier.create(manager.authenticate(authenticationToken))
                .expectNextMatches(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_user")))
                .verifyComplete();
    }

    @Test
    void jwtDecoderEnforcesIssuerAndAudience() {
        var decoder = securityConfig.jwtDecoder();

        Object field = ReflectionTestUtils.getField(decoder, "jwtValidator");
        assertNotNull(field);

        @SuppressWarnings("unchecked")
        OAuth2TokenValidator<Jwt> validator = (OAuth2TokenValidator<Jwt>) field;

        Jwt valid = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("iss", "expected-issuer")
                .audience(List.of("expected-aud"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertEquals(OAuth2TokenValidatorResult.success(), validator.validate(valid));

        Jwt invalid = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("iss", "wrong")
                .audience(List.of("wrong"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertTrue(validator.validate(invalid).hasErrors());
    }

    @Test
    void buildJwkWebClientUsesSslContextWhenCertificateProvided() throws Exception {
        Path certificate = createTempCertificate();
        ReflectionTestUtils.setField(securityConfig, "jwkTrustedCertificatePath", certificate.toString());

        WebClient client = ReflectionTestUtils.invokeMethod(securityConfig, "buildJwkWebClient");
        assertNotNull(client);
    }

    @Test
    void buildJwkWebClientThrowsWhenCertificateMissing() {
        ReflectionTestUtils.setField(securityConfig, "jwkTrustedCertificatePath", "/nonexistent/cert.pem");
        assertThrows(IllegalStateException.class, () ->
                ReflectionTestUtils.invokeMethod(securityConfig, "buildJwkWebClient"));
    }

    @Test
    void buildSslContextLoadsCertificate() throws Exception {
        Path certificate = createTempCertificate();
        Object context = ReflectionTestUtils.invokeMethod(securityConfig, "buildSslContext", certificate.toString());
        assertNotNull(context);
    }

    @Test
    void springSecurityFilterChainBuildsSuccessfully() {
        ServerHttpSecurity http = ServerHttpSecurity.http();
        ReactiveAuthenticationManager authManager = mock(ReactiveAuthenticationManager.class);
        ServerAuthenticationConverter converter = new ServerBearerTokenAuthenticationConverter();

        SecurityWebFilterChain chain = securityConfig.springSecurityFilterChain(http, authManager, converter);
        assertNotNull(chain);
    }

    private Path createTempCertificate() throws Exception {
        String pem = String.join("\n",
                "-----BEGIN CERTIFICATE-----",
                "MIIDwjCCAiqgAwIBAgIJALltwq8fWjfoMA0GCSqGSIb3DQEBDAUAMA8xDTALBgNV",
                "BAMTBFRlc3QwHhcNMjUxMTIwMTY1MzI1WhcNMjYxMTIwMTY1MzI1WjAPMQ0wCwYD",
                "VQQDEwRUZXN0MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEAyzEXoKvj",
                "368oylQWj45RZFhejFoxG1o8vHkttkfG/Djn1pA9WZDidHvsUm7v9/afCBJ5gtHu",
                "Rim3JytX1VPuloqA++D+KKwKOIBQ9tuiTF8dMTOYIRcnGinaz38pAvPJuOAbE6li",
                "VAAAmpsqtIfHhJfcvtBA0FL19EXskn60+TjWZ2zgQxTClQi+FyOXBPchYH9npSWf",
                "NPvCNrgZ3SME9kxB9D3rea06WAi3Foc6gh2IArU3PNt1dTQSZj+D+rV4+PSZ0mjk",
                "uwSkFn+ngO7P62/bXzWqgI12uQkem7DtL1X5F33+eC5flg4FXLHRGqu2TLesPzIL",
                "HdpOEAVlxcOlRdfT9ICyypHnrW/Uz2EA++W9BcyJdHreF6p4srHpSK3W/Nlnqq2K",
                "4qgjIHckoIVcksRGMf/8NMmaIWrDqwzYJXR9gUwG81DCeyZ5boE86hy8MaqrOyH0",
                "J797prUCr4oqttWFUEeRSsFUMg+eFeJMkxvlEEi1AHTVTxntccYSHy/xAgMBAAGj",
                "ITAfMB0GA1UdDgQWBBTb2LFD56lvHyL+/gMgNJmb14L2VzANBgkqhkiG9w0BAQwF",
                "AAOCAYEAZRSbfMyB+SbT9aCLR2fat3kfOuZiZmlkH5GUfhZFznqNInPm0nbWknCw",
                "tcG+BTQ0JYZ4IRjsXzfsJ/EuU21iU1f4pHRRJU+TwCoYMY4W7RduojsyWH+NXLd5",
                "o8B9kpOfR8RvRBA/YqxfyVnUmNZnY5WB1RSd8gxlPHkHrIMjGC/VHCUWo1JbIywk",
                "I9bz+CK4CvP14AX3vsZ4Ffv313eq11gTKbgdxApPhSB7Mh8v7bPPhSoExuXBqiOw",
                "5dSUrW8kWB20yqc5cWCVqq/lHPj2lhlQzBEZRL9Wr12RU2wa5FaGZesVaHIFen3V",
                "z7hezrACxAR1N8XByumJL+sgAzv+VYsLpBPjZgPf2vix+1VozrBNAPCcIoJCj8D+",
                "yKUhlwxdX4M2Y/PHQuexYdAMghKQnEGXDlocj5WQcQTMmblS7FF2upBdm8OHW3h+",
                "ZL2p9a1NSEEfYJ17+xwr0Ijwp11PMxn2g5HPot6ctyjiduePbGlJQAEJbXpR6O0Q",
                "mY+RthWD",
                "-----END CERTIFICATE-----");

        Path tempFile = java.nio.file.Files.createTempFile("cert", ".pem");
        java.nio.file.Files.writeString(tempFile, pem);
        tempFile.toFile().deleteOnExit();
        return tempFile;
    }
}

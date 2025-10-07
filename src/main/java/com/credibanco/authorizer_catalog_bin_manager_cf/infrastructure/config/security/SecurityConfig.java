package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.DelegatingServerAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.net.ssl.TrustManagerFactory;

@Configuration
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${internal.jwt.expected-issuer}")
    private String expectedIssuer;            // authorizer-gateway

    @Value("${internal.jwt.required-audience}")
    private String requiredAudience;          // catalog-api

    @Value("${internal.jwt.jwk.trusted-cert-path:}")
    private String jwkTrustedCertificatePath;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            ReactiveAuthenticationManager authManager,
                                                            ServerAuthenticationConverter xAuthConverter) {


        AuthenticationWebFilter xAuthFilter = new AuthenticationWebFilter(authManager);
        xAuthFilter.setServerAuthenticationConverter(xAuthConverter);

        xAuthFilter.setRequiresAuthenticationMatcher(ServerWebExchangeMatchers.anyExchange());

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/actuator/**", "/health").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(xAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    /**
     * Convierte header X-Auth-Token en un BearerTokenAuthenticationToken.
     * Si no viene, se deja vacío (y terminará en 401 por falta de credenciales).
     */
    @Bean
    public ServerAuthenticationConverter xAuthTokenConverter() {
        ServerBearerTokenAuthenticationConverter bearerConverter = new ServerBearerTokenAuthenticationConverter();

        ServerAuthenticationConverter xAuthConverter = exchange -> {
            String token = exchange.getRequest().getHeaders().getFirst("X-Auth-Token");
            if (!StringUtils.hasText(token)) {
                return Mono.empty();
            }
            return Mono.just(new BearerTokenAuthenticationToken(token));
        };

        return new DelegatingServerAuthenticationConverter(xAuthConverter, bearerConverter);
    }

    /**
     * AuthenticationManager basado en JWT (reactivo) que usa nuestro decoder + mapeo de authorities.
     */
    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(ReactiveJwtDecoder decoder) {
        JwtReactiveAuthenticationManager manager = new JwtReactiveAuthenticationManager(decoder);

        // Converter para mapear roles -> authorities
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // 1) roles (claim plano)
            List<String> roles = jwt.getClaimAsStringList("roles");
            // 2) fallback realm_access.roles
            if (roles == null) {
                Map<String, Object> ra = jwt.getClaim("realm_access");
                if (ra != null && ra.get("roles") instanceof List<?> l) {
                    roles = (List<String>) (List<?>) l;
                }
            }
            if (roles == null) roles = List.of();

            // opcional: también podrías usar JwtGrantedAuthoritiesConverter si tuvieras "scope" o "scp"
            Collection<GrantedAuthority> auths = roles.stream()
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            return auths;
        });

        manager.setJwtAuthenticationConverter(jwt -> Mono.just(jwtConverter.convert(jwt)));
        return manager;
    }

    /**
     * Reactive decoder + validadores de issuer y audience.
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .webClient(buildJwkWebClient())
                .build();

        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefault(); // exp/nbf, etc.

        // <<< CAMBIO CLAVE: NO usar jwt.getIssuer() >>>
        OAuth2TokenValidator<Jwt> withIssuer = jwt -> {
            String iss = jwt.getClaimAsString("iss");
            return expectedIssuer.equals(iss)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_issuer", "Unexpected iss", null));
        };

        OAuth2TokenValidator<Jwt> withAudience = jwt -> {
            var aud = jwt.getAudience();
            return (aud != null && aud.contains(requiredAudience))
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_audience", "aud must contain " + requiredAudience, null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults, withIssuer, withAudience));
        return decoder;
    }

    private WebClient buildJwkWebClient() {
        HttpClient httpClient = HttpClient.create();

        if (StringUtils.hasText(jwkTrustedCertificatePath)) {
            try {
                httpClient = httpClient.secure(ssl -> ssl.sslContext(buildSslContext(jwkTrustedCertificatePath)));
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to initialize SSL context for JWK requests", ex);
            }
        }

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private SslContext buildSslContext(String certificatePath) throws Exception {
        Path path = Path.of(certificatePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Certificate file does not exist: " + certificatePath);
        }

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        try (InputStream inputStream = Files.newInputStream(path)) {
            int index = 0;
            for (Certificate certificate : certificateFactory.generateCertificates(inputStream)) {
                keyStore.setCertificateEntry("cert-" + index++, certificate);
            }
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        return SslContextBuilder.forClient()
                .trustManager(trustManagerFactory)
                .build();
    }


}
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class ActorProvider {

    public Mono<String> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth.getPrincipal() instanceof Jwt)
                .cast(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken.class)
                .map(jwtAuth -> (Jwt) jwtAuth.getPrincipal())
                .map(jwt -> Optional.ofNullable(jwt.getClaimAsString("preferred_username"))
                        .orElse(jwt.getSubject()))
                .switchIfEmpty(Mono.just("system"));
    }

    public Mono<String> currentEmail() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth.getPrincipal() instanceof Jwt)
                .cast(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken.class)
                .map(jwtAuth -> (Jwt) jwtAuth.getPrincipal())
                .map(jwt -> jwt.getClaimAsString("email"))
                .defaultIfEmpty(null);
    }

    public Mono<String> correlationId() {
        // El gateway propaga X-Correlation-Id; si quieres leerlo aquí:
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth.getPrincipal() instanceof Jwt)
                .cast(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken.class)
                .map(jwtAuth -> (Jwt) jwtAuth.getPrincipal())
                .map(jwt -> jwt.getClaimAsString("cid"))
                .defaultIfEmpty(null);
    }
}
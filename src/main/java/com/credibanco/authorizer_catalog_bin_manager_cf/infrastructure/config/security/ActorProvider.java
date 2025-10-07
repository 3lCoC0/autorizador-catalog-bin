package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.logging.CorrelationWebFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Component
public class ActorProvider {

    public Mono<String> currentUserId() {
        Mono<String> fromContext = Mono.deferContextual(ctx -> {
            String fromCtx = ctx.getOrDefault(CorrelationWebFilter.CTX_USER, null);
            if (fromCtx != null && !fromCtx.isBlank()) {
                log.debug("ActorProvider - resolved user from Reactor context: {}", fromCtx);
                return Mono.just(fromCtx);
            }
            return Mono.empty();
        });

        Mono<String> fromSecurity = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth.getPrincipal() instanceof Jwt)
                .cast(org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken.class)
                .map(jwtAuth -> (Jwt) jwtAuth.getPrincipal())
                .map(jwt -> Optional.ofNullable(jwt.getClaimAsString("preferred_username"))
                        .orElse(jwt.getSubject()))
                .doOnNext(user -> log.debug("ActorProvider - resolved user from security context: {}", user));

        return fromContext.switchIfEmpty(fromSecurity);
    }

}
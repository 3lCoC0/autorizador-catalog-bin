package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.logging.CorrelationWebFilter;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;


class ActorProviderTest {

    private final ActorProvider actorProvider = new ActorProvider();

    @Test
    void resolvesUserFromReactorContextFirst() {
        StepVerifier.create(actorProvider.currentUserId()
                        .contextWrite(ctx -> ctx.put(CorrelationWebFilter.CTX_USER, "reactor-user")))
                .expectNext("reactor-user")
                .verifyComplete();
    }

    @Test
    void resolvesUserFromSecurityContextWhenContextEmpty() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "preferred-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        Authentication authentication = new JwtAuthenticationToken(jwt);

        Mono<String> result = actorProvider.currentUserId()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        StepVerifier.create(result)
                .expectNext("preferred-user")
                .verifyComplete();
    }

    @Test
    void fallsBackToSubjectWhenPreferredUsernameMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "the-subject")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        Authentication authentication = new JwtAuthenticationToken(jwt);

        Mono<String> result = actorProvider.currentUserId()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        StepVerifier.create(result)
                .expectNext("the-subject")
                .verifyComplete();
    }
}

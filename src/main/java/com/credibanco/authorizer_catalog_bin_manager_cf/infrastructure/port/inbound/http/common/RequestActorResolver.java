package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.common;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security.ActorProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestActorResolver {

    private final ActorProvider actorProvider;

    public Mono<ActorResolution> resolve(ServerRequest request, String bodyUser, String operation) {
        return resolveCandidate(request, operation, bodyUser);
    }

    public Mono<ActorResolution> resolve(ServerRequest request, String operation) {
        return resolveCandidate(request, operation, null);
    }

    private Mono<ActorResolution> resolveCandidate(ServerRequest request, String operation, String candidate) {
        return Mono.defer(() -> Mono.justOrEmpty(normalize(candidate))
                        .map(ActorResolution::fromBody)
                        .switchIfEmpty(Mono.justOrEmpty(normalize(request.headers().firstHeader("X-User")))
                                .map(ActorResolution::fromHeader))
                        .switchIfEmpty(actorProvider.currentUserId()
                                .flatMap(user -> Mono.justOrEmpty(normalize(user)))
                                .map(ActorResolution::fromSecurityContext))
                        .defaultIfEmpty(ActorResolution.none()))
                .doOnNext(resolution -> log.info("{} - actor resolved from {}: {}",
                        operation,
                        resolution.source().description(),
                        resolution.printableActor()));
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public record ActorResolution(String actor, ActorSource source) {
        private static final String NO_ACTOR = "<none>";
        private static final ActorResolution NONE = new ActorResolution(null, ActorSource.NONE);

        static ActorResolution fromBody(String actor) { return new ActorResolution(actor, ActorSource.REQUEST_BODY); }
        static ActorResolution fromHeader(String actor) { return new ActorResolution(actor, ActorSource.HEADER); }
        static ActorResolution fromSecurityContext(String actor) { return new ActorResolution(actor, ActorSource.SECURITY_CONTEXT); }
        static ActorResolution none() { return NONE; }

        public String printableActor() {
            return StringUtils.hasText(actor) ? actor : NO_ACTOR;
        }

        public String actorOrNull() {
            return StringUtils.hasText(actor) ? actor : null;
        }
    }

    public enum ActorSource {
        REQUEST_BODY("request body"),
        HEADER("X-User header"),
        SECURITY_CONTEXT("security context"),
        NONE("<none>");

        private final String description;

        ActorSource(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }
    }
}

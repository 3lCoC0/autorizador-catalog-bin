// infrastructure/port/inbound/http/subtype/handler/SubtypeHandler.java
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security.ActorProvider;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.http.ApiResponses.*;
@Slf4j
@Component
@RequiredArgsConstructor
public class SubtypeHandler {

    private final CreateSubtypeUseCase createUC;
    private final ListSubtypesUseCase listUC;
    private final ValidationUtil validation;
    private final UpdateSubtypeBasicsUseCase updateUC;
    private final GetSubtypeUseCase getUC;
    private final ChangeSubtypeStatusUseCase changeStatusUC;
    private final ActorProvider actorProvider;
    private static long elapsedMs(long t0) { return (System.nanoTime() - t0) / 1_000_000; }

    private Mono<String> resolveUser(ServerRequest req, String fromBody, String operation) {
        return Mono.defer(() -> {
                    String fromRequest = toNullable(fromBody);
                    if (fromRequest != null) {
                        log.debug("{} - actor from request body: {}", operation, fromRequest);
                        return Mono.just(fromRequest);
                    }
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    String headerUser = toNullable(req.headers().firstHeader("X-User"));
                    if (headerUser != null) {
                        log.info("{} - actor from header X-User: {}", operation, headerUser);
                        return Mono.just(headerUser);
                    }
                    return Mono.empty();
                }))
                .switchIfEmpty(actorProvider.currentUserId()
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .doOnNext(user -> log.info("{} - actor from security context: {}", operation, user)));
    }

    private String printableActor(String user) {
        return (user == null || user.isBlank()) ? "<none>" : user;
    }

    private String toNullable(String value) {
        return (value != null && !value.isBlank()) ? value : null;
    }

    private SubtypeResponse toResponse(com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype s) {
        return new SubtypeResponse(
                s.subtypeCode(), s.bin(), s.name(), s.description(),
                s.status(), s.ownerIdType(), s.ownerIdNumber(),
                s.binExt(), s.binEfectivo(), s.subtypeId(),
                s.createdAt(), s.updatedAt(), s.updatedBy()
        );
    }



    public Mono<ServerResponse> create(ServerRequest req) {
        long t0 = System.nanoTime();
        return req.bodyToMono(SubtypeCreateRequest.class)
                .doOnSubscribe(s -> log.info("SUBTYPE:create:recv"))
                .flatMap(r -> validation.validate(r, AppError.SUBTYPE_INVALID_DATA)
                        .flatMap(valid -> {
                            log.debug("SUBTYPE:create:validated bin={} code={} ownerType={} ext='{}'",
                                    valid.bin(), valid.subtypeCode(), valid.ownerIdType(), valid.binExt());
                            return resolveUser(req, valid.createdBy(), "subtype.create")
                                    .defaultIfEmpty("")
                                    .flatMap(user -> {
                                        log.info("subtype.create - actor used={}", printableActor(user));
                                        return createUC.execute(
                                                valid.subtypeCode(), valid.bin(), valid.name(), valid.description(),
                                                valid.ownerIdType(), valid.ownerIdNumber(), valid.binExt(),
                                                toNullable(user)
                                        );
                                    });
                        }))
                .map(this::toResponse)
                .doOnSuccess(b -> log.info("SUBTYPE:create:done bin={} code={} status={} elapsedMs={}",
                        b.bin(), b.subtypeCode(), b.status(), elapsedMs(t0)))
                .flatMap(body -> ServerResponse.created(
                                req.uriBuilder().path("/{bin}/{code}").build(body.bin(), body.subtypeCode()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", body)));
    }

    public Mono<ServerResponse> listByBin(ServerRequest req) {
        long t0 = System.nanoTime();
        final String bin = req.pathVariable("bin");
        String status = req.queryParam("status").orElse(null);
        int page      = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size      = req.queryParam("size").map(Integer::parseInt).orElse(20);
        if (!bin.chars().allMatch(Character::isDigit) || bin.length() < 6 || bin.length() > 9) {
            return Mono.error(new AppException(AppError.SUBTYPE_INVALID_DATA,
                    "El path variable 'bin' debe ser numérico de 6 a 9 dígitos"));
        }
        log.info("SUBTYPE:list:recv bin={} status={} page={} size={}", bin, status, page, size);

        return listUC.execute(bin, null, status, page, size)
                .map(this::toResponse)
                .collectList()
                .doOnSuccess(list -> log.info("SUBTYPE:list:done bin={} status={} count={} elapsedMs={}",
                        bin, status, list.size(), elapsedMs(t0)))
                .flatMap(list -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", list)));
    }

    public Mono<ServerResponse> update(ServerRequest req) {
        long t0 = System.nanoTime();
        String bin  = req.pathVariable("bin");
        String code = req.pathVariable("code");

        return req.bodyToMono(SubtypeUpdateRequest.class)
                .doOnSubscribe(s -> log.info("SUBTYPE:update:recv bin={} code={}", bin, code))
                .flatMap(r -> validation.validate(r, AppError.SUBTYPE_INVALID_DATA)
                        .flatMap(valid -> {
                            log.debug("SUBTYPE:update:validated bin={} code={} ext='{}'", bin, code, valid.binExt());
                            return resolveUser(req, valid.updatedBy(), "subtype.update")
                                    .defaultIfEmpty("")
                                    .flatMap(user -> {
                                        log.info("subtype.update - actor used={}", printableActor(user));
                                        return updateUC.execute(
                                                bin, code, valid.name(), valid.description(),
                                                valid.ownerIdType(), valid.ownerIdNumber(), valid.binExt(),
                                                toNullable(user) // opcional
                                        );
                                    });
                        }))
                .map(this::toResponse)
                .doOnSuccess(b -> log.info("SUBTYPE:update:done bin={} code={} elapsedMs={}",
                        b.bin(), b.subtypeCode(), elapsedMs(t0)))
                .flatMap(body -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", body)));
    }

    public Mono<ServerResponse> get(ServerRequest req) {
        long t0 = System.nanoTime();
        String bin  = req.pathVariable("bin");
        String code = req.pathVariable("code");
        log.info("SUBTYPE:get:recv bin={} code={}", bin, code);

        return getUC.execute(bin, code)
                .map(this::toResponse)
                .doOnSuccess(b -> log.info("SUBTYPE:get:done bin={} code={} status={} elapsedMs={}",
                        b.bin(), b.subtypeCode(), b.status(), elapsedMs(t0)))
                .flatMap(body -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", body)));
    }

    public Mono<ServerResponse> changeStatus(ServerRequest req) {
        long t0 = System.nanoTime();
        String bin  = req.pathVariable("bin");
        String code = req.pathVariable("code");

        return req.bodyToMono(SubtypeStatusRequest.class)
                .doOnSubscribe(s -> log.info("SUBTYPE:status:recv bin={} code={}", bin, code))

                .flatMap(r -> validation.validate(r, AppError.SUBTYPE_INVALID_DATA)
                        .flatMap(valid -> resolveUser(req, valid.updatedBy(), "subtype.changeStatus")
                                .defaultIfEmpty("")
                                .flatMap(user -> {
                                    log.info("subtype.changeStatus - actor used={}", printableActor(user));
                                    return changeStatusUC.execute(bin, code, valid.status(), toNullable(user));
                                })))
                .map(this::toResponse)
                .doOnSuccess(b -> log.info("SUBTYPE:status:done bin={} code={} newStatus={} elapsedMs={}",
                        b.bin(), b.subtypeCode(), b.status(), elapsedMs(t0)))
                .flatMap(body -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Se cambió el STATUS del subtype correctamente", body)));
    }
}

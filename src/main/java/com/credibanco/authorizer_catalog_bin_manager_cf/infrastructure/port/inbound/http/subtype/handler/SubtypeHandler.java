// infrastructure/port/inbound/http/subtype/handler/SubtypeHandler.java
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
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
    private static long elapsedMs(long t0) { return (System.nanoTime() - t0) / 1_000_000; }

    private String resolveUser(ServerRequest req, String fromBody) {
        String hdr = req.headers().firstHeader("X-User");
        return (fromBody != null && !fromBody.isBlank())
                ? fromBody
                : (hdr != null && !hdr.isBlank() ? hdr : null);
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
                .flatMap(r -> validation.validate(r, AppError.SUBTYPE_INVALID_DATA))
                .flatMap(r -> {
                    log.debug("SUBTYPE:create:validated bin={} code={} ownerType={} ext='{}'",
                            r.bin(), r.subtypeCode(), r.ownerIdType(), r.binExt());
                    return createUC.execute(
                            r.subtypeCode(), r.bin(), r.name(), r.description(),
                            r.ownerIdType(), r.ownerIdNumber(), r.binExt(),
                            resolveUser(req, r.createdBy())
                    );
                })
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
        String bin    = req.pathVariable("bin");
        String status = req.queryParam("status").orElse(null);
        int page      = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size      = req.queryParam("size").map(Integer::parseInt).orElse(20);

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
                .flatMap(r -> validation.validate(r, AppError.SUBTYPE_INVALID_DATA))
                .flatMap(r -> {
                    log.debug("SUBTYPE:update:validated bin={} code={} ext='{}'", bin, code, r.binExt());
                    return updateUC.execute(
                            bin, code, r.name(), r.description(),
                            r.ownerIdType(), r.ownerIdNumber(), r.binExt(),
                            resolveUser(req, r.updatedBy()) // opcional
                    );
                })
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
                // ✅ nuevo validador con código "04"
                .flatMap(r -> validation.validate(r, AppError.SUBTYPE_INVALID_DATA))
                .flatMap(r -> changeStatusUC.execute(bin, code, r.status(), resolveUser(req, r.updatedBy())))
                .map(this::toResponse)
                .doOnSuccess(b -> log.info("SUBTYPE:status:done bin={} code={} newStatus={} elapsedMs={}",
                        b.bin(), b.subtypeCode(), b.status(), elapsedMs(t0)))
                .flatMap(body -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Se cambió el STATUS del subtype correctamente", body)));
    }
}

// infrastructure/port/inbound/http/subtype/handler/SubtypeHandler.java
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.http.ApiResponses.*;

@Component
@RequiredArgsConstructor
public class SubtypeHandler {

    private final CreateSubtypeUseCase createUC;
    private final ListSubtypesUseCase listUC;
    private final ValidationUtil validation;
    private final UpdateSubtypeBasicsUseCase updateUC;
    private final GetSubtypeUseCase getUC;
    private final ChangeSubtypeStatusUseCase changeStatusUC;

    private SubtypeResponse toResponse(com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype s) {
        return new SubtypeResponse(
                s.subtypeCode(), s.bin(), s.name(), s.description(),
                s.status(), s.ownerIdType(), s.ownerIdNumber(),
                s.binExt(), s.binEfectivo(), s.subtypeId(),
                s.createdAt(), s.updatedAt(), s.updatedBy()
        );
    }

    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(SubtypeCreateRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> createUC.execute(
                        r.subtypeCode(), r.bin(), r.name(), r.description(),
                        r.ownerIdType(), r.ownerIdNumber(), r.binExt(), r.createdBy()
                ))
                .map(this::toResponse)
                .flatMap(body -> ServerResponse.created(
                                req.uriBuilder().path("/{bin}/{code}").build(body.bin(), body.subtypeCode()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", body)));
    }

    public Mono<ServerResponse> listByBin(ServerRequest req) {
        String bin    = req.pathVariable("bin");
        String status = req.queryParam("status").orElse(null);
        int page = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = req.queryParam("size").map(Integer::parseInt).orElse(20);

        return listUC.execute(bin, null, status, page, size)
                .map(this::toResponse)
                .collectList()
                .flatMap(list -> jsonOk().bodyValue(okEnvelope(req, "Operación exitosa", list)));
    }

    public Mono<ServerResponse> update(ServerRequest req) {
        String bin  = req.pathVariable("bin");
        String code = req.pathVariable("code");

        return req.bodyToMono(SubtypeUpdateRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> updateUC.execute(
                        bin, code,
                        r.name(), r.description(), r.ownerIdType(), r.ownerIdNumber(), r.binExt(),
                        r.updatedBy()
                ))
                .map(this::toResponse)
                .flatMap(body -> jsonOk().bodyValue(okEnvelope(req, "Operación exitosa", body)));
    }

    public Mono<ServerResponse> get(ServerRequest req) {
        String bin  = req.pathVariable("bin");
        String code = req.pathVariable("code");

        return getUC.execute(bin, code)
                .map(this::toResponse)
                .flatMap(body -> jsonOk().bodyValue(okEnvelope(req, "Operación exitosa", body)));
    }

    public Mono<ServerResponse> changeStatus(ServerRequest req) {
        String bin  = req.pathVariable("bin");
        String code = req.pathVariable("code");

        return req.bodyToMono(SubtypeStatusRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> changeStatusUC.execute(bin, code, r.status(), r.updatedBy()))
                .map(this::toResponse)
                .flatMap(body -> jsonOk().bodyValue(okEnvelope(req, "Se cambió el STATUS del subtype correctamente", body)));
    }
}

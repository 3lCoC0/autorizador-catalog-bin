package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.ChangeSubtypeStatusUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.CreateSubtypeUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.GetSubtypeUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.ListSubtypesUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound.UpdateSubtypeBasicsUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto.SubtypeCreateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto.SubtypeResponse;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto.SubtypeStatusRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto.SubtypeUpdateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SubtypeHandler {

    private final CreateSubtypeUseCase createUC;
    private final ListSubtypesUseCase listUC;
    private final ValidationUtil validation;
    private final UpdateSubtypeBasicsUseCase updateUC;
    private final GetSubtypeUseCase getUC;
    private final ChangeSubtypeStatusUseCase changeStatusUC;

    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(SubtypeCreateRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> createUC.execute(
                        r.subtypeCode(), r.bin(), r.name(), r.description(),
                        r.ownerIdType(), r.ownerIdNumber(), r.binExt(), r.createdBy()
                ))
                .map(s -> new SubtypeResponse(
                        s.subtypeCode(), s.bin(), s.name(), s.description(),
                        s.status(), s.ownerIdType(), s.ownerIdNumber(),
                        s.binExt(), s.binEfectivo(), s.subtypeId(),
                        s.createdAt(), s.updatedAt(), s.updatedBy()
                ))
                .flatMap(resp -> ServerResponse.created(
                                req.uriBuilder().path("/{bin}/{code}")
                                        .build(resp.bin(), resp.subtypeCode()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> listByBin(ServerRequest req) {
        String bin    = req.pathVariable("bin");
        String status = req.queryParam("status").orElse(null);
        int page = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = req.queryParam("size").map(Integer::parseInt).orElse(20);

        var body = listUC.execute(bin, null, status, page, size)
                .map(s -> new SubtypeResponse(
                        s.subtypeCode(), s.bin(), s.name(), s.description(),
                        s.status(), s.ownerIdType(), s.ownerIdNumber(),
                        s.binExt(), s.binEfectivo(), s.subtypeId(),
                        s.createdAt(), s.updatedAt(), s.updatedBy()
                ));

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body, SubtypeResponse.class);
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
                .map(s -> new SubtypeResponse(
                        s.subtypeCode(), s.bin(), s.name(), s.description(),
                        s.status(), s.ownerIdType(), s.ownerIdNumber(),
                        s.binExt(), s.binEfectivo(), s.subtypeId(),
                        s.createdAt(), s.updatedAt(), s.updatedBy()
                ))
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> get(ServerRequest req) {
        String bin  = req.pathVariable("bin");
        String code = req.pathVariable("code");

        return getUC.execute(bin, code)
                .map(s -> new SubtypeResponse(
                        s.subtypeCode(), s.bin(), s.name(), s.description(),
                        s.status(), s.ownerIdType(), s.ownerIdNumber(),
                        s.binExt(), s.binEfectivo(), s.subtypeId(),
                        s.createdAt(), s.updatedAt(), s.updatedBy()
                ))
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> changeStatus(ServerRequest req) {
        String bin  = req.pathVariable("bin");
        String code = req.pathVariable("code");

        return req.bodyToMono(SubtypeStatusRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> changeStatusUC.execute(bin, code, r.status(), r.updatedBy()))
                .map(s -> new SubtypeResponse(
                        s.subtypeCode(), s.bin(), s.name(), s.description(),
                        s.status(), s.ownerIdType(), s.ownerIdNumber(),
                        s.binExt(), s.binEfectivo(), s.subtypeId(),
                        s.createdAt(), s.updatedAt(), s.updatedBy()
                ))
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }
}

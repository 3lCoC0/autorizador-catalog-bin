package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.handler;


import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.use_case.CreateSubtypeUseCase;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class SubtypeHandler {

    private final CreateSubtypeUseCase createUC;
    private final UpdateSubtypeBasicsUseCase updateUC;
    private final ChangeSubtypeStatusUseCase changeStatusUC;
    private final GetSubtypeUseCase getUC;
    private final ListSubtypesUseCase listUC;
    private final DeleteSubtypeUseCase deleteUC;

    public SubtypeHandler(CreateSubtypeUseCase createUC,
                          UpdateSubtypeBasicsUseCase updateUC,
                          ChangeSubtypeStatusUseCase changeStatusUC,
                          GetSubtypeUseCase getUC,
                          ListSubtypesUseCase listUC,
                          DeleteSubtypeUseCase deleteUC) {
        this.createUC = createUC;
        this.updateUC = updateUC;
        this.changeStatusUC = changeStatusUC;
        this.getUC = getUC;
        this.listUC = listUC;
        this.deleteUC = deleteUC;
    }

    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(SubtypeCreateRequest.class)
                .flatMap(r -> createUC.execute(
                        r.subtypeCode(), r.bin(), r.name(), r.descripcion(),
                        r.ownerIdType(), r.ownerIdNumber(), r.binExt(), r.createdBy()
                ))
                .map(this::toResponse)
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp))
                .onErrorResume(e -> badRequest("Error creando SUBTYPE", e));
    }

    public Mono<ServerResponse> get(ServerRequest req) {
        String bin = req.pathVariable("bin");
        String code = req.pathVariable("code");
        return getUC.execute(bin, code)
                .map(this::toResponse)
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp))
                .onErrorResume(e -> badRequest("No se pudo obtener SUBTYPE", e));
    }

    public Mono<ServerResponse> list(ServerRequest req) {
        String bin    = req.queryParam("bin").orElse(null);
        String code   = req.queryParam("code").orElse(null);
        String status = req.queryParam("status").orElse(null);
        int page = Integer.parseInt(req.queryParam("page").orElse("0"));
        int size = Integer.parseInt(req.queryParam("size").orElse("20"));
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(listUC.execute(bin, code, status, page, size).map(this::toResponse), SubtypeResponse.class)
                .onErrorResume(e -> badRequest("No se pudo listar SUBTYPEs", e));
    }

    public Mono<ServerResponse> update(ServerRequest req) {
        String bin = req.pathVariable("bin");
        String code = req.pathVariable("code");
        return req.bodyToMono(SubtypeUpdateRequest.class)
                .flatMap(r -> updateUC.execute(
                        bin, code,
                        r.name(), r.descripcion(), r.ownerIdType(), r.ownerIdNumber(), r.binExt(),
                        r.updatedBy()
                ))
                .map(this::toResponse)
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp))
                .onErrorResume(e -> badRequest("Error actualizando SUBTYPE", e));
    }

    public Mono<ServerResponse> changeStatus(ServerRequest req) {
        String bin = req.pathVariable("bin");
        String code = req.pathVariable("code");
        return req.bodyToMono(SubtypeStatusRequest.class)
                .flatMap(r -> changeStatusUC.execute(bin, code, r.status(), r.updatedBy()))
                .map(this::toResponse)
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp))
                .onErrorResume(e -> badRequest("Error cambiando estado del SUBTYPE", e));
    }

    public Mono<ServerResponse> delete(ServerRequest req) {
        String bin = req.pathVariable("bin");
        String code = req.pathVariable("code");
        String by = req.queryParam("by").orElse("system");
        return deleteUC.execute(bin, code, by)
                .map(this::toResponse)
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp))
                .onErrorResume(e -> badRequest("Error inactivando SUBTYPE", e));
    }

    private SubtypeResponse toResponse(Subtype s) {
        return new SubtypeResponse(
                s.subtypeCode(), s.bin(), s.name(), s.descripcion(), s.status(),
                s.ownerIdType(), s.ownerIdNumber(), s.binExt(), s.binEfectivo(),
                s.subtypeId(), s.createdAt(), s.updatedAt(), s.updatedBy()
        );
    }

    private Mono<ServerResponse> badRequest(String title, Throwable e) {
        var problem = new ProblemDetails("about:blank", title, 400, e.getMessage());
        return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).bodyValue(problem);
    }
}
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PlanHandler {

    private final CreatePlanUseCase createUC;
    private final GetPlanUseCase getUC;
    private final ListPlansUseCase listUC;
    private final UpdatePlanUseCase updateUC;
    private final ChangePlanStatusUseCase changeStatusUC;
    private final AddPlanItemUseCase addItemUC;
    private final RemovePlanItemUseCase removeItemUC;
    private final ListPlanItemsUseCase listItemsUC;
    private final AssignPlanToSubtypeUseCase assignUC;
    private final ValidationUtil validation;

    // === CRUD Plan ===

    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(PlanCreateRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> createUC.execute(r.code(), r.name(), r.validationMode(), r.description(), r.updatedBy()))
                .map(this::toResp)
                .flatMap(resp -> ServerResponse.created(
                                req.uriBuilder().path("/{code}").build(resp.code()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> get(ServerRequest req) {
        var code = req.pathVariable("code");
        return getUC.execute(code)
                .map(this::toResp)
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> list(ServerRequest req) {
        var status = req.queryParam("status").orElse(null);
        var q      = req.queryParam("q").orElse(null);
        int page   = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size   = req.queryParam("size").map(Integer::parseInt).orElse(20);
        var body   = listUC.execute(status, q, page, size).map(this::toResp);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body, PlanResponse.class);
    }

    public Mono<ServerResponse> update(ServerRequest req) {
        var code = req.pathVariable("code");
        return req.bodyToMono(PlanUpdateRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> updateUC.execute(code, r.name(), r.description(), r.validationMode(), r.updatedBy()))
                .map(this::toResp)
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> changeStatus(ServerRequest req) {
        var code = req.pathVariable("code");
        return req.bodyToMono(PlanStatusRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> changeStatusUC.execute(code, r.status(), r.updatedBy()))
                .map(this::toResp)
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    // === Items (devuelven dominio mapeado a DTO) ===

    public Mono<ServerResponse> addItem(ServerRequest req) {
        return req.bodyToMono(PlanItemRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> addItemUC.addValue(r.planCode(), r.value(), r.updatedBy()))
                .map(this::toItemResp)
                .flatMap(resp -> ServerResponse.created(
                                // Nota: el primer placeholder corresponde a {code} pero tenemos planId; lo mantenemos como referencia
                                // Si quieres una Location RESTful real, podrías usar "/v1/plans/{code}/items" desde el router
                                req.uriBuilder().path("/{code}/items/{id}")
                                        .build(resp.planId(), resp.planItemId())
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> removeItem(ServerRequest req) {
        var code  = req.pathVariable("code");
        var value = req.pathVariable("value");
        return removeItemUC.removeValue(code, value)
                .then(ServerResponse.noContent().build()); // 204
    }

    public Mono<ServerResponse> listItems(ServerRequest req) {
        var code = req.pathVariable("code");
        int page = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = req.queryParam("size").map(Integer::parseInt).orElse(100);
        var body = listItemsUC.list(code, page, size).map(this::toItemResp);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body, PlanItemResponse.class);
    }

    // === Subtype ← Plan (devuelve link de dominio como DTO) ===

    public Mono<ServerResponse> assignToSubtype(ServerRequest req) {
        return req.bodyToMono(AssignPlanRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> assignUC.assign(r.subtypeCode(), r.planCode(), r.updatedBy()))
                .map(this::toLinkResp)
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    // === Mapeos a DTO ===

    private PlanResponse toResp(CommercePlan p) {
        return new PlanResponse(
                p.planId(), p.code(), p.name(), p.validationMode(),
                p.description(), p.status(), p.createdAt(), p.updatedAt(), p.updatedBy()
        );
    }

    private PlanItemResponse toItemResp(PlanItem i) {
        return new PlanItemResponse(
                i.planItemId(), i.planId(), i.value(), i.createdAt(), i.updatedAt(), i.updatedBy()
        );
    }

    private SubtypePlanLinkResponse toLinkResp(SubtypePlanLink l) {
        return new SubtypePlanLinkResponse(
                l.subtypeCode(), l.planId(), l.createdAt(), l.updatedAt(), l.updatedBy()
        );
    }
}

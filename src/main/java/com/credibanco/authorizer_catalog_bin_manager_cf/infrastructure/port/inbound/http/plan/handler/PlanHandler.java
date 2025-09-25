package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
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

    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(PlanCreateRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> createUC.execute(r.code(), r.name(), r.validationMode(), r.description(), r.updatedBy()))
                .map(this::toResp)
                .flatMap(resp -> ServerResponse.created(req.uriBuilder().path("/{code}").build(resp.code()))
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(resp));
    }

    public Mono<ServerResponse> get(ServerRequest req) {
        var code = req.pathVariable("code");
        return getUC.execute(code)
                .map(this::toResp)
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp));
    }

    public Mono<ServerResponse> list(ServerRequest req) {
        var status = req.queryParam("status").orElse(null);
        var q      = req.queryParam("q").orElse(null);
        int page   = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size   = req.queryParam("size").map(Integer::parseInt).orElse(20);
        var body   = listUC.execute(status,q,page,size).map(this::toResp);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(body, PlanResponse.class);
    }

    public Mono<ServerResponse> update(ServerRequest req) {
        var code = req.pathVariable("code");
        return req.bodyToMono(PlanUpdateRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> updateUC.execute(code, r.name(), r.description(), r.updatedBy()))
                .map(this::toResp)
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp));
    }

    public Mono<ServerResponse> changeStatus(ServerRequest req) {
        var code = req.pathVariable("code");
        return req.bodyToMono(PlanStatusRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> changeStatusUC.execute(code, r.status(), r.updatedBy()))
                .map(this::toResp)
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp));
    }

    // Items (value depende del modo del plan)
    public Mono<ServerResponse> addItem(ServerRequest req) {
        return req.bodyToMono(PlanItemRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> addItemUC.addValue(r.planCode(), r.value(), r.updatedBy()))
                .then(ServerResponse.ok().build());
    }

    public Mono<ServerResponse> removeItem(ServerRequest req) {
        var code  = req.pathVariable("code");
        var value = req.pathVariable("value");
        return removeItemUC.removeValue(code, value)
                .then(ServerResponse.noContent().build());
    }

    public Mono<ServerResponse> listItems(ServerRequest req) {
        var code = req.pathVariable("code");
        int page = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = req.queryParam("size").map(Integer::parseInt).orElse(100);
        var body = listItemsUC.list(code, page, size);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(body, String.class);
    }

    // Subtype → Plan
    public Mono<ServerResponse> assignToSubtype(ServerRequest req) {
        return req.bodyToMono(AssignPlanRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> assignUC.assign(r.subtypeCode(), r.planCode(), r.updatedBy()))
                .then(ServerResponse.ok().build());
    }

    private PlanResponse toResp(CommercePlan p) {
        return new PlanResponse(
                p.planId(), p.code(), p.name(), p.validationMode(),
                p.description(), p.status(), p.createdAt(), p.updatedAt(), p.updatedBy()
        );
    }
}

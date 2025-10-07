package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.model.PlanItemsBulkResult;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.http.ApiResponses;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.logging.CorrelationWebFilter;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.common.RequestActorResolver;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanHandler {

    private final CreatePlanUseCase createUC;
    private final GetPlanUseCase getUC;
    private final ListPlansUseCase listUC;
    private final UpdatePlanUseCase updateUC;
    private final ChangePlanStatusUseCase changeStatusUC;
    private final AddPlanItemUseCase addItemUC;
    private final ListPlanItemsUseCase listItemsUC;
    private final AssignPlanToSubtypeUseCase assignUC;
    private final ValidationUtil validation;
    private final ChangePlanItemStatusUseCase changeItemStatusUC;
    private final RequestActorResolver actorResolver;


    private Mono<ServerResponse> ok(ServerRequest req, String detail, Object data) {
        var envelope = ApiResponses.okEnvelope(req, detail, data);
        return ApiResponses.jsonOk().bodyValue(envelope);
    }



    public Mono<ServerResponse> create(ServerRequest req) {
        String cid = req.headers().firstHeader(CorrelationWebFilter.CID);
        log.info("create plan - IN cid={}", cid);

        return req.bodyToMono(PlanCreateRequest.class)
                .flatMap(r -> validation.validate(r, AppError.PLAN_INVALID_DATA)) // ← "18"
                .flatMap(r -> actorResolver.resolve(req, r.updatedBy(), "plan.create")
                        .flatMap(resolution -> createUC.execute(
                                r.code(), r.name(), r.validationMode(), r.description(),
                                resolution.actorOrNull()
                        )))
                .map(this::toResp)
                .flatMap(resp -> {
                    log.info("create plan - OK cid={} code={}", cid, resp.code());
                    return ApiResponses.jsonCreated(req, resp.code())
                            .bodyValue(ApiResponses.okEnvelope(req, "Plan creado", resp));
                });
    }

    public Mono<ServerResponse> get(ServerRequest req) {
        String code = req.pathVariable("code");
        String cid = req.headers().firstHeader(CorrelationWebFilter.CID);
        log.info("get plan - IN cid={} code={}", cid, code);

        return getUC.execute(code)
                .map(this::toResp)
                .flatMap(resp -> {
                    log.info("get plan - OK cid={} code={}", cid, code);
                    return ok(req, "Consulta exitosa", resp);
                });
    }

    public Mono<ServerResponse> list(ServerRequest req) {
        var status = req.queryParam("status").orElse(null);
        var q      = req.queryParam("q").orElse(null);
        int page   = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size   = req.queryParam("size").map(Integer::parseInt).orElse(20);

        String cid = req.headers().firstHeader(CorrelationWebFilter.CID);
        log.info("list plans - IN cid={} status={} q={} page={} size={}", cid, status, q, page, size);

        Flux<PlanResponse> body = listUC.execute(status, q, page, size).map(this::toResp);

        return body.collectList().flatMap(list -> {
            log.info("list plans - OK cid={} count={}", cid, list.size());
            String detail = list.isEmpty() ? "Sin planes para el filtro" : "Consulta exitosa";
            return ok(req, detail, list);
        });
    }

    public Mono<ServerResponse> update(ServerRequest req) {
        var code = req.pathVariable("code");
        String cid = req.headers().firstHeader(CorrelationWebFilter.CID);
        log.info("update plan - IN cid={} code={}", cid, code);

        return req.bodyToMono(PlanUpdateRequest.class)
                .flatMap(r -> validation.validate(r, AppError.PLAN_INVALID_DATA)) // ← "18"
                .flatMap(r -> actorResolver.resolve(req, r.updatedBy(), "plan.update")
                        .flatMap(resolution -> updateUC.execute(code, r.name(), r.description(), r.validationMode(),
                                resolution.actorOrNull())))
                .map(this::toResp)
                .flatMap(resp -> {
                    log.info("update plan - OK cid={} code={}", cid, code);
                    return ok(req, "Plan actualizado", resp);
                });
    }

    public Mono<ServerResponse> changeStatus(ServerRequest req) {
        String cid = req.headers().firstHeader(CorrelationWebFilter.CID);
        log.info("change plan status - IN cid={}", cid);

        return req.bodyToMono(PlanStatusRequest.class)
                .flatMap(r -> validation.validate(r, AppError.PLAN_INVALID_DATA)) // ← "18"
                .flatMap(r -> actorResolver.resolve(req, r.updatedBy(), "plan.changeStatus")
                        .flatMap(resolution -> changeStatusUC.execute(r.planCode(), r.status(),
                                resolution.actorOrNull())))
                .map(this::toResp)
                .flatMap(resp -> {
                    log.info("change plan status - OK cid={} code={}", cid, resp.code());
                    return ok(req, "Estado de plan actualizado", resp);
                });
    }


    public Mono<ServerResponse> addItem(ServerRequest req) {
        String cid = req.headers().firstHeader(CorrelationWebFilter.CID);
        log.info("add plan item(s) - IN cid={}", cid);

        return req.bodyToMono(PlanItemRequest.class)
                .flatMap(r -> validation.validate(r, AppError.PLAN_ITEM_INVALID_DATA)) // ← "21"
                .flatMap(r -> actorResolver.resolve(req, r.updatedBy(), "plan.addItem")
                        .flatMap(resolution -> {
                            String by = resolution.actorOrNull();

                            boolean hasBulk   = r.values() != null && !r.values().isEmpty();
                            boolean hasSingle = r.value() != null && !r.value().isBlank();

                            if (!hasBulk && !hasSingle) {
                                return Mono.error(new AppException(
                                        AppError.PLAN_ITEM_INVALID_DATA,
                                        "Debe enviar 'value' (single) o 'values' (bulk)"
                                ));
                            }

                            if (hasBulk) {
                                return addItemUC.addMany(r.planCode(), r.values(), by)
                                        .map(this::toBulkDto)
                                        .flatMap(resp -> ApiResponses.jsonOk()
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(ApiResponses.okEnvelope(req, "Carga masiva procesada", resp)));
                            }

                            return addItemUC.addValue(r.planCode(), r.value(), by)
                                    .map(this::toItemResp)
                                    .flatMap(resp -> {
                                        log.info("add plan item (single) - OK cid={} planId={} itemId={}",
                                                cid, resp.planId(), resp.planItemId());
                                        return ServerResponse.created(
                                                        req.uriBuilder().path("/{code}/items/{id}")
                                                                .build(resp.planId(), resp.planItemId())
                                                )
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(ApiResponses.okEnvelope(req, "Ítem agregado", resp));
                                    });
                        }));
    }


    private PlanItemsBulkResponse toBulkDto(PlanItemsBulkResult r) {
        return new PlanItemsBulkResponse(
                r.planCode(),
                r.requested(),
                r.inserted(),
                r.duplicates(),
                r.invalid(),
                r.invalidValues(),
                r.duplicateValues()
        );
    }


    public Mono<ServerResponse> changeItemStatus(ServerRequest req) {
        String cid = req.headers().firstHeader(CorrelationWebFilter.CID);
        log.info("change item status - IN cid={}", cid);

        return req.bodyToMono(PlanItemStatus.class)
                .flatMap(r -> validation.validate(r, AppError.PLAN_ITEM_INVALID_DATA)) // ← "21"
                .flatMap(r -> actorResolver.resolve(req, r.updatedBy(), "plan.changeItemStatus")
                        .flatMap(resolution -> changeItemStatusUC.execute(
                                r.planCode(), r.value(), r.status(),
                                resolution.actorOrNull())))
                .map(this::toItemResp)
                .flatMap(resp -> {
                    log.info("change item status - OK cid={} planId={} itemId={}", cid, resp.planId(), resp.planItemId());
                    return ok(req, "Estado de ítem actualizado", resp);
                });
    }

    public Mono<ServerResponse> listItems(ServerRequest req) {
        var code = req.pathVariable("code");
        int page = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = req.queryParam("size").map(Integer::parseInt).orElse(100);
        String status = req.queryParam("status")
                .map(String::trim).map(String::toUpperCase)
                .filter(s -> !"ALL".equals(s)).orElse("A");

        String cid = req.headers().firstHeader(CorrelationWebFilter.CID);
        log.info("list plan items - IN cid={} code={} status={} page={} size={}", cid, code, status, page, size);

        return listItemsUC.list(code, page, size, status).map(this::toItemResp).collectList()
                .flatMap(list -> {
                    String detail = list.isEmpty()
                            ? "A".equals(status)
                            ? "El plan no tiene ítems activos"
                            : "El plan no tiene ítems para el filtro"
                            : "Consulta exitosa";
                    log.info("list plan items - OK cid={} code={} count={}", cid, code, list.size());
                    return ok(req, detail, list);
                });
    }


    public Mono<ServerResponse> assignToSubtype(ServerRequest req) {
        String cid = req.headers().firstHeader(CorrelationWebFilter.CID);
        log.info("assign plan to subtype - IN cid={}", cid);

        return req.bodyToMono(AssignPlanRequest.class)
                .flatMap(r -> validation.validate(r, AppError.PLAN_ASSIGNMENT_INVALID_DATA)) // ← "23"
                .flatMap(r -> actorResolver.resolve(req, r.updatedBy(), "plan.assignToSubtype")
                        .flatMap(resolution -> assignUC.assign(r.subtypeCode(), r.planCode(),
                                resolution.actorOrNull())))
                .map(this::toLinkResp)
                .flatMap(resp -> {
                    log.info("assign plan to subtype - OK cid={} subtype={} planId={}",
                            cid, resp.subtypeCode(), resp.planId());
                    return ok(req, "Plan asignado al SUBTYPE", resp);
                });
    }


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
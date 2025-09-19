package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RuleHandler {
    private final CreateValidationUseCase createV;
    private final UpdateValidationUseCase updateV;
    private final ChangeValidationStatusUseCase changeVStatus;
    private final GetValidationUseCase getV;
    private final ListValidationsUseCase listV;
    private final MapRuleUseCase mapRuleUC;
    private final ListRulesForSubtypeUseCase listRulesUC;
    private final ValidationUtil validation;

    public Mono<ServerResponse> createValidation(ServerRequest req) {
        return req.bodyToMono(ValidationCreateRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> createV.execute(r.code(), r.description(), r.dataType(),
                        r.valueFlag(), r.valueNum(), r.valueText()))
                .map(this::toResp)
                .flatMap(resp -> ServerResponse.created(
                                req.uriBuilder().path("/{code}").build(resp.code()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> updateValidation(ServerRequest req) {
        var code = req.pathVariable("code");
        return req.bodyToMono(ValidationUpdateRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> updateV.execute(code, r.description(), r.valueFlag(), r.valueNum(), r.valueText()))
                .map(this::toResp)
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp));
    }

    public Mono<ServerResponse> changeValidationStatus(ServerRequest req) {
        var code = req.pathVariable("code");
        return req.bodyToMono(ValidationStatusRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> changeVStatus.execute(code, r.status()))
                .map(this::toResp)
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp));
    }

    public Mono<ServerResponse> getValidation(ServerRequest req) {
        var code = req.pathVariable("code");
        return getV.execute(code).map(this::toResp)
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp));
    }

    public Mono<ServerResponse> listValidations(ServerRequest req) {
        var status = req.queryParam("status").orElse(null);
        var q      = req.queryParam("q").orElse(null);
        int page   = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size   = req.queryParam("size").map(Integer::parseInt).orElse(20);
        var body = listV.execute(status, q, page, size).map(this::toResp);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(body, ValidationResponse.class);
    }

    public Mono<ServerResponse> attachRule(ServerRequest req) {
        var st  = req.pathVariable("subtypeCode");
        var eff = req.pathVariable("binEfectivo");
        var code= req.pathVariable("code");
        return req.bodyToMono(MapRuleRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> mapRuleUC.attach(st, eff, code, r.priority(), r.updatedBy()))
                .flatMap(m -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(m));
    }

    public Mono<ServerResponse> changeRuleStatus(ServerRequest req) {
        var st  = req.pathVariable("subtypeCode");
        var eff = req.pathVariable("binEfectivo");
        var code= req.pathVariable("code");
        return req.bodyToMono(ValidationStatusRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> mapRuleUC.changeStatus(st, eff, code, r.status(), "api"))
                .flatMap(m -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(m));
    }

    public Mono<ServerResponse> listRulesForSubtype(ServerRequest req) {
        var st  = req.pathVariable("subtypeCode");
        var eff = req.pathVariable("binEfectivo");
        var status = req.queryParam("status").orElse("A");
        int page = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = req.queryParam("size").map(Integer::parseInt).orElse(100);
        var body = listRulesUC.execute(st, eff, status, page, size).map(this::toResp);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(body, ValidationResponse.class);
    }

    private ValidationResponse toResp(Validation v) {
        return new ValidationResponse(
                v.validationId(), v.code(), v.description(), v.dataType(),
                v.valueFlag(), v.valueNum(), v.valueText(),
                v.status(), v.validFrom(), v.validTo(), v.createdAt(), v.updatedAt()
        );
    }
}

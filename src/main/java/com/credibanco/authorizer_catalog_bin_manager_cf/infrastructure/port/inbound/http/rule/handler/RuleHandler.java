// infrastructure/port/inbound/http/rule/handler/RuleHandler.java
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security.ActorProvider;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import static com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.http.ApiResponses.*;

@Slf4j
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
    private final ActorProvider actorProvider;

    private static long ms(long t0) { return (System.nanoTime() - t0) / 1_000_000; }

    public Mono<ServerResponse> createValidation(ServerRequest req) {
        long t0 = System.nanoTime();
        return req.bodyToMono(ValidationCreateRequest.class)
                .doOnSubscribe(s -> log.info("RULES:validation:create:recv"))
                .flatMap(r -> validation.validate(r, AppError.RULES_VALIDATION_INVALID_DATA))
                .flatMap(r -> resolveUser(req, r.createdBy(), "rules.validation.create")
                        .defaultIfEmpty("")
                        .flatMap(user -> {
                            log.info("rules.validation.create - actor used={}", printableActor(user));
                            ValidationDataType type;
                            try {
                                type = ValidationDataType.fromJson(r.dataType());
                            } catch (IllegalArgumentException e) {
                                return Mono.error(new AppException(AppError.RULES_VALIDATION_INVALID_DATA, e.getMessage()));
                            }
                            return createV.execute(r.code(), r.description(), type, toNullable(user));
                        }))
                .map(this::toResp)
                .doOnSuccess(v -> log.info("RULES:validation:create:done code={} elapsedMs={}", v.code(), ms(t0)))
                .flatMap(resp -> ServerResponse.created(req.uriBuilder().path("/{code}").build(resp.code()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", resp)));
    }


    public Mono<ServerResponse> updateValidation(ServerRequest req) {
        long t0 = System.nanoTime();
        var code = req.pathVariable("code");
        return req.bodyToMono(ValidationUpdateRequest.class)
                .doOnSubscribe(s -> log.info("RULES:validation:update:recv code={}", code))
                .flatMap(r -> validation.validate(r, AppError.RULES_VALIDATION_INVALID_DATA))
                .flatMap(r -> resolveUser(req, r.updatedBy(), "rules.validation.update")
                        .defaultIfEmpty("")
                        .flatMap(user -> {
                            log.info("rules.validation.update - actor used={}", printableActor(user));
                            return updateV.execute(code, r.description(), toNullable(user));
                        }))
                .map(this::toResp)
                .doOnSuccess(v -> log.info("RULES:validation:update:done code={} elapsedMs={}", v.code(), ms(t0)))
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", resp)));
    }


    public Mono<ServerResponse> changeValidationStatus(ServerRequest req) {
        long t0 = System.nanoTime();
        var code = req.pathVariable("code");
        return req.bodyToMono(ValidationStatusRequest.class)
                .doOnSubscribe(s -> log.info("RULES:validation:status:recv code={}", code))
                .flatMap(r -> validation.validate(r, AppError.RULES_VALIDATION_INVALID_DATA))
                .flatMap(r -> resolveUser(req, r.updatedBy(), "rules.validation.changeStatus")
                        .defaultIfEmpty("")
                        .flatMap(user -> {
                            log.info("rules.validation.changeStatus - actor used={}", printableActor(user));
                            return changeVStatus.execute(code, r.status(), toNullable(user));
                        }))
                .map(this::toResp)
                .doOnSuccess(v -> log.info("RULES:validation:status:done code={} newStatus={} elapsedMs={}",
                        v.code(), v.status(), ms(t0)))
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", resp)));
    }

    public Mono<ServerResponse> getValidation(ServerRequest req) {
        long t0 = System.nanoTime();
        var code = req.pathVariable("code");
        log.info("RULES:validation:get:recv code={}", code);
        return getV.execute(code)
                .map(this::toResp)
                .doOnSuccess(v -> log.info("RULES:validation:get:done code={} elapsedMs={}", v.code(), ms(t0)))
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", resp)));
    }

    public Mono<ServerResponse> listValidations(ServerRequest req) {
        long t0 = System.nanoTime();
        var status = req.queryParam("status").orElse(null);
        var q      = req.queryParam("q").orElse(null);
        int page   = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size   = req.queryParam("size").map(Integer::parseInt).orElse(20);
        log.info("RULES:validation:list:recv status={} q={} page={} size={}", status, q, page, size);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(listV.execute(status, q, page, size).map(this::toResp), ValidationResponse.class)
                .doOnTerminate(() -> log.info("RULES:validation:list:done elapsedMs={}", ms(t0)));
    }


    public Mono<ServerResponse> attachRule(ServerRequest req) {
        long t0 = System.nanoTime();
        return req.bodyToMono(MapRuleRequest.class)
                .doOnSubscribe(s -> log.info("RULES:map:attach:recv"))
                .flatMap(r -> validation.validate(r, AppError.RULES_MAP_INVALID_DATA))
                .flatMap(r -> resolveUser(req, r.updatedBy(), "rules.map.attach")
                        .defaultIfEmpty("")
                        .flatMap(user -> {
                            log.info("rules.map.attach - actor used={}", printableActor(user));
                            return mapRuleUC.attach(r.subtypeCode(), r.bin(), r.code(), r.value(), toNullable(user));
                        }))
                .map(this::toMapResp)
                .doOnSuccess(m -> log.info("RULES:map:attach:done st={} bin={} valId={} elapsedMs={}",
                        m.subtypeCode(), m.bin(), m.validationId(), ms(t0)))
                .flatMap(body -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", body)));
    }


    public Mono<ServerResponse> changeRuleStatus(ServerRequest req) {
        long t0 = System.nanoTime();
        var st  = req.pathVariable("subtypeCode");
        var bin = req.pathVariable("bin");
        var code= req.pathVariable("code");
        return req.bodyToMono(ValidationStatusRequest.class)
                .doOnSubscribe(s -> log.info("RULES:map:status:recv st={} bin={} code={}", st, bin, code))
                .flatMap(r -> validation.validate(r, AppError.RULES_MAP_INVALID_DATA))
                .flatMap(r -> resolveUser(req, r.updatedBy(), "rules.map.changeStatus")
                        .defaultIfEmpty("")
                        .flatMap(user -> {
                            log.info("rules.map.changeStatus - actor used={}", printableActor(user));
                            return mapRuleUC.changeStatus(st, bin, code, r.status(), toNullable(user));
                        }))
                .map(this::toMapResp)
                .doOnSuccess(m -> log.info("RULES:map:status:done st={} bin={} valId={} newStatus={} elapsedMs={}",
                        m.subtypeCode(), m.bin(), m.validationId(), m.status(), ms(t0)))
                .flatMap(body -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", body)));
    }


    public Mono<ServerResponse> listRulesForSubtype(ServerRequest req) {
        long t0 = System.nanoTime();
        var st  = req.pathVariable("subtypeCode");
        var eff = req.pathVariable("binEfectivo");
        var status = req.queryParam("status").orElse("A");
        int page = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = req.queryParam("size").map(Integer::parseInt).orElse(100);

        log.info("RULES:map:list:recv st={} eff={} status={} page={} size={}", st, eff, status, page, size);

        return listRulesUC.execute(st, eff, status, page, size)
                .map(this::toMapResp)
                .collectList()
                .flatMap(list -> {
                    String detail = buildDetailForList(st, eff, status, list);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(okEnvelope(req, detail, list));
                })
                .doOnTerminate(() -> log.info("RULES:map:list:done elapsedMs={}", (System.nanoTime()-t0)/1_000_000));
    }

    public Mono<ServerResponse> listRulesForSubtypeBySubtype(ServerRequest req) {
        long t0 = System.nanoTime();
        var st  = req.pathVariable("subtypeCode");
        var status = req.queryParam("status").orElse("A");
        int page = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = req.queryParam("size").map(Integer::parseInt).orElse(100);

        log.info("RULES:map:list:recv st={} status={} page={} size={}", st, status, page, size);

        return listRulesUC.execute(st, status, page, size)
                .map(this::toMapResp)
                .collectList()
                .flatMap(list -> {
                    String detail = buildDetailForList(st, null, status, list);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(okEnvelope(req, detail, list));
                })
                .doOnTerminate(() -> log.info("RULES:map:list:done st={} elapsedMs={}", st, (System.nanoTime()-t0)/1_000_000));
    }


    private String buildDetailForList(String subtypeCode, String binEfectivoOrNull, String status, List<?> list) {
        boolean empty = list == null || list.isEmpty();
        boolean onlyActive = "A".equalsIgnoreCase(status);
        if (empty && onlyActive) {
            if (binEfectivoOrNull != null) {
                return "El SUBTYPE " + subtypeCode + " con BIN " + binEfectivoOrNull + " no tiene reglas activas.";
            }
            return "El SUBTYPE " + subtypeCode + " no tiene reglas activas.";
        }
        if (empty) {
            if (binEfectivoOrNull != null) {
                return "El SUBTYPE " + subtypeCode + " con BIN " + binEfectivoOrNull + " no tiene reglas con el filtro aplicado (status=" + status + ").";
            }
            return "El SUBTYPE " + subtypeCode + " no tiene reglas con el filtro aplicado (status=" + status + ").";
        }
        // Cuando sí hay elementos, un detalle neutro
        return "Operación exitosa";
    }

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

    private ValidationResponse toResp(Validation v) {
        return new ValidationResponse(
                v.validationId(), v.code(), v.description(), v.dataType(),
                v.status(), v.validFrom(), v.validTo(), v.createdAt(), v.updatedAt(), v.updatedBy()
        );
    }

    private ValidationMapResponse toMapResp(ValidationMap m) {
        return new ValidationMapResponse(
                m.mapId(), m.subtypeCode(), m.bin(), m.validationId(),
                m.status(), m.valueFlag(), m.valueNum(), m.valueText(),
                m.createdAt(), m.updatedAt(), m.updatedBy()
        );
    }
}

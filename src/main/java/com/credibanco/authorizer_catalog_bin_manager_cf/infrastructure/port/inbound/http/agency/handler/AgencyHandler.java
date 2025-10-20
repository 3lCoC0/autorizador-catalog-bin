package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security.ActorProvider;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.common.RequestActorResolver;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import com.credibanco.authorizer_catalog_bin_manager_cf.shared.validation.TextNormalizer;
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
public class AgencyHandler {

    private final CreateAgencyUseCase createUC;
    private final UpdateAgencyUseCase updateUC;
    private final ChangeAgencyStatusUseCase changeStatusUC;
    private final GetAgencyUseCase getUC;
    private final ListAgenciesUseCase listUC;
    private final ValidationUtil validation;
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

    private String normalize(String value) {
        return TextNormalizer.uppercaseAndRemoveAccents(value);
    }

    public Mono<ServerResponse> create(ServerRequest req) {
        long t0 = System.nanoTime();
        return req.bodyToMono(AgencyCreateRequest.class)
                .doOnSubscribe(s -> log.info("AGENCY:create:recv"))
                .flatMap(r -> validation.validate(r, AppError.AGENCY_INVALID_DATA)
                        .flatMap(valid -> resolveUser(req, valid.createdBy(), "agency.create")
                                .defaultIfEmpty("")
                                .map(user -> {
                                    log.info("agency.create - actor used={}", printableActor(user));
                                    return Agency.createNew(
                                            normalize(valid.subtypeCode()), normalize(valid.agencyCode()), normalize(valid.name()),
                                            normalize(valid.agencyNit()), normalize(valid.address()), normalize(valid.phone()), normalize(valid.municipalityDaneCode()),
                                            normalize(valid.embosserHighlight()), normalize(valid.embosserPins()),
                                            normalize(valid.cardCustodianPrimary()), normalize(valid.cardCustodianPrimaryId()),
                                            normalize(valid.cardCustodianSecondary()), normalize(valid.cardCustodianSecondaryId()),
                                            normalize(valid.pinCustodianPrimary()), normalize(valid.pinCustodianPrimaryId()),
                                            normalize(valid.pinCustodianSecondary()), normalize(valid.pinCustodianSecondaryId()),
                                            normalize(valid.description()), toNullable(user)
                                    );
                                })))
                .onErrorMap(IllegalArgumentException.class,
                        e -> new AppException(AppError.AGENCY_INVALID_DATA, e.getMessage()))
                .flatMap(createUC::execute)
                .map(this::toResponse)
                .doOnSuccess(a -> log.info("AGENCY:create:done st={} ag={} status={} elapsedMs={}",
                        a.subtypeCode(), a.agencyCode(), a.status(), elapsedMs(t0)))
                .flatMap(body -> ServerResponse.created(
                                req.uriBuilder().path("/{subtypeCode}/{agencyCode}")
                                        .build(body.subtypeCode(), body.agencyCode()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", body)));
    }

    public Mono<ServerResponse> update(ServerRequest req) {
        long t0 = System.nanoTime();
        String subtypeCode = normalize(req.pathVariable("subtypeCode"));
        String agencyCode  = normalize(req.pathVariable("agencyCode"));

        return req.bodyToMono(AgencyUpdateRequest.class)
                .doOnSubscribe(s -> log.info("AGENCY:update:recv st={} ag={}", subtypeCode, agencyCode))
                .flatMap(r -> validation.validate(r, AppError.AGENCY_INVALID_DATA)
                        .flatMap(valid -> resolveUser(req, valid.updatedBy(), "agency.update")
                                .defaultIfEmpty("")
                                .map(user -> {
                                    log.info("agency.update - actor used={}", printableActor(user));
                                    return new Agency(
                                            subtypeCode, agencyCode, normalize(valid.name()),
                                            normalize(valid.agencyNit()), normalize(valid.address()), normalize(valid.phone()), normalize(valid.municipalityDaneCode()),
                                            normalize(valid.embosserHighlight()), normalize(valid.embosserPins()),
                                            normalize(valid.cardCustodianPrimary()), normalize(valid.cardCustodianPrimaryId()),
                                            normalize(valid.cardCustodianSecondary()), normalize(valid.cardCustodianSecondaryId()),
                                            normalize(valid.pinCustodianPrimary()), normalize(valid.pinCustodianPrimaryId()),
                                            normalize(valid.pinCustodianSecondary()), normalize(valid.pinCustodianSecondaryId()),
                                            normalize(valid.description()), "A", null, null, toNullable(user)
                                    );
                                })))
                .onErrorMap(IllegalArgumentException.class,
                        e -> new AppException(AppError.AGENCY_INVALID_DATA, e.getMessage()))
                .flatMap(updateUC::execute)
                .map(this::toResponse)
                .doOnSuccess(a -> log.info("AGENCY:update:done st={} ag={} elapsedMs={}",
                        a.subtypeCode(), a.agencyCode(), elapsedMs(t0)))
                .flatMap(body -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", body)));
    }

    public Mono<ServerResponse> changeStatus(ServerRequest req) {
        long t0 = System.nanoTime();
        String subtypeCode = normalize(req.pathVariable("subtypeCode"));
        String agencyCode  = normalize(req.pathVariable("agencyCode"));

        return req.bodyToMono(AgencyStatusRequest.class)
                .doOnSubscribe(s -> log.info("AGENCY:status:recv st={} ag={}", subtypeCode, agencyCode))
                .flatMap(r -> validation.validate(r, AppError.AGENCY_INVALID_DATA)
                        .flatMap(valid -> resolveUser(req, valid.updatedBy(), "agency.changeStatus")
                                .defaultIfEmpty("")
                                .flatMap(user -> {
                                    log.info("agency.changeStatus - actor used={}", printableActor(user));
                                    return changeStatusUC.execute(
                                            subtypeCode, agencyCode, normalize(valid.status()), toNullable(user));
                                })))
                .map(this::toResponse)
                .doOnSuccess(a -> log.info("AGENCY:status:done st={} ag={} newStatus={} elapsedMs={}",
                        a.subtypeCode(), a.agencyCode(), a.status(), elapsedMs(t0)))
                .flatMap(body -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Se cambió el STATUS de la agencia correctamente", body)));
    }


    public Mono<ServerResponse> get(ServerRequest req) {
        long t0 = System.nanoTime();
        String subtypeCode = normalize(req.pathVariable("subtypeCode"));
        String agencyCode  = normalize(req.pathVariable("agencyCode"));
        log.info("AGENCY:get:recv st={} ag={}", subtypeCode, agencyCode);
        return getUC.execute(subtypeCode, agencyCode)
                .map(this::toResponse)
                .doOnSuccess(a -> log.info("AGENCY:get:done st={} ag={} status={} elapsedMs={}",
                        a.subtypeCode(), a.agencyCode(), a.status(), elapsedMs(t0)))
                .flatMap(body -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", body)));
    }

    public Mono<ServerResponse> list(ServerRequest req) {
        long t0 = System.nanoTime();
        String subtype = req.queryParam("subtypeCode")
                .orElseThrow(() -> new IllegalArgumentException("subtypeCode es requerido"));
        String rawStatus = req.queryParam("status").map(String::trim).map(String::toUpperCase)
                .filter(s -> !s.isEmpty()).orElse(null);
        final String status = "ALL".equals(rawStatus) ? null : rawStatus;
        if (status != null && !"A".equals(status) && !"I".equals(status))
            return Mono.error(new IllegalArgumentException("status debe ser 'A', 'I' o 'ALL'"));
        String search = req.queryParam("search").orElse(null);
        int page = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = req.queryParam("size").map(Integer::parseInt).orElse(20);
        log.info("AGENCY:list:recv st={} status={} page={} size={}", subtype, status, page, size);
        return listUC.execute(subtype, status, search, page, size)
                .map(this::toResponse)
                .collectList()
                .doOnSuccess(list -> log.info("AGENCY:list:done st={} status={} count={} elapsedMs={}",
                        subtype, status, list.size(), elapsedMs(t0)))
                .flatMap(list -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Operación exitosa", list)));
    }


    private AgencyResponse toResponse(Agency a) {
        return new AgencyResponse(
                a.subtypeCode(), a.agencyCode(), a.name(),
                a.agencyNit(), a.address(), a.phone(), a.municipalityDaneCode(),
                a.embosserHighlight(), a.embosserPins(),
                a.cardCustodianPrimary(), a.cardCustodianPrimaryId(),
                a.cardCustodianSecondary(), a.cardCustodianSecondaryId(),
                a.pinCustodianPrimary(), a.pinCustodianPrimaryId(),
                a.pinCustodianSecondary(), a.pinCustodianSecondaryId(),
                a.description(), a.status(), a.createdAt(), a.updatedAt(), a.updatedBy()
        );
    }
}

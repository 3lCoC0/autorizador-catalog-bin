package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.common.RequestActorResolver;
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
public class AgencyHandler {

    private final CreateAgencyUseCase createUC;
    private final UpdateAgencyUseCase updateUC;
    private final ChangeAgencyStatusUseCase changeStatusUC;
    private final GetAgencyUseCase getUC;
    private final ListAgenciesUseCase listUC;
    private final ValidationUtil validation;
    private final RequestActorResolver actorResolver;

    private static long elapsedMs(long t0) { return (System.nanoTime() - t0) / 1_000_000; }


    public Mono<ServerResponse> create(ServerRequest req) {
        long t0 = System.nanoTime();
        return req.bodyToMono(AgencyCreateRequest.class)
                .doOnSubscribe(s -> log.info("AGENCY:create:recv"))
                // ✅ validar con código "07"
                .flatMap(r -> validation.validate(r, AppError.AGENCY_INVALID_DATA)
                        .flatMap(valid -> actorResolver.resolve(req, valid.createdBy(), "agency.create")
                                .map(resolution -> Agency.createNew(
                                        valid.subtypeCode(), valid.agencyCode(), valid.name(),
                                        valid.agencyNit(), valid.address(), valid.phone(), valid.municipalityDaneCode(),
                                        valid.embosserHighlight(), valid.embosserPins(),
                                        valid.cardCustodianPrimary(), valid.cardCustodianPrimaryId(),
                                        valid.cardCustodianSecondary(), valid.cardCustodianSecondaryId(),
                                        valid.pinCustodianPrimary(), valid.pinCustodianPrimaryId(),
                                        valid.pinCustodianSecondary(), valid.pinCustodianSecondaryId(),
                                        valid.description(), resolution.actorOrNull()
                                ))))
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
        String subtypeCode = req.pathVariable("subtypeCode");
        String agencyCode  = req.pathVariable("agencyCode");

        return req.bodyToMono(AgencyUpdateRequest.class)
                .doOnSubscribe(s -> log.info("AGENCY:update:recv st={} ag={}", subtypeCode, agencyCode))
                // ✅ validar con código "07"
                .flatMap(r -> validation.validate(r, AppError.AGENCY_INVALID_DATA)
                        .flatMap(valid -> actorResolver.resolve(req, valid.updatedBy(), "agency.update")
                                .map(resolution -> new Agency(
                                        subtypeCode, agencyCode, valid.name(),
                                        valid.agencyNit(), valid.address(), valid.phone(), valid.municipalityDaneCode(),
                                        valid.embosserHighlight(), valid.embosserPins(),
                                        valid.cardCustodianPrimary(), valid.cardCustodianPrimaryId(),
                                        valid.cardCustodianSecondary(), valid.cardCustodianSecondaryId(),
                                        valid.pinCustodianPrimary(), valid.pinCustodianPrimaryId(),
                                        valid.pinCustodianSecondary(), valid.pinCustodianSecondaryId(),
                                        valid.description(), "A", null, null, resolution.actorOrNull()
                                ))))
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
        String subtypeCode = req.pathVariable("subtypeCode");
        String agencyCode  = req.pathVariable("agencyCode");

        return req.bodyToMono(AgencyStatusRequest.class)
                .doOnSubscribe(s -> log.info("AGENCY:status:recv st={} ag={}", subtypeCode, agencyCode))
                // ✅ validar con código "07"
                .flatMap(r -> validation.validate(r, AppError.AGENCY_INVALID_DATA)
                        .flatMap(valid -> actorResolver.resolve(req, valid.updatedBy(), "agency.changeStatus")
                                .flatMap(resolution -> changeStatusUC.execute(
                                        subtypeCode, agencyCode, valid.status(), resolution.actorOrNull()))))
                .map(this::toResponse)
                .doOnSuccess(a -> log.info("AGENCY:status:done st={} ag={} newStatus={} elapsedMs={}",
                        a.subtypeCode(), a.agencyCode(), a.status(), elapsedMs(t0)))
                .flatMap(body -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(okEnvelope(req, "Se cambió el STATUS de la agencia correctamente", body)));
    }


    public Mono<ServerResponse> get(ServerRequest req) {
        long t0 = System.nanoTime();
        String subtypeCode = req.pathVariable("subtypeCode");
        String agencyCode  = req.pathVariable("agencyCode");
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

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto.*;
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
public class AgencyHandler {

    private final CreateAgencyUseCase createUC;
    private final UpdateAgencyUseCase updateUC;
    private final ChangeAgencyStatusUseCase changeStatusUC;
    private final GetAgencyUseCase getUC;
    private final ListAgenciesUseCase listUC;
    private final ValidationUtil validation;

    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(AgencyCreateRequest.class)
                .flatMap(validation::validate)
                .map(r -> Agency.createNew(
                        r.subtypeCode(), r.agencyCode(), r.name(),
                        r.agencyNit(), r.address(), r.phone(), r.municipalityDaneCode(),
                        r.embosserHighlight(), r.embosserPins(),
                        r.cardCustodianPrimary(), r.cardCustodianPrimaryId(),
                        r.cardCustodianSecondary(), r.cardCustodianSecondaryId(),
                        r.pinCustodianPrimary(), r.pinCustodianPrimaryId(),
                        r.pinCustodianSecondary(), r.pinCustodianSecondaryId(),
                        r.description(), r.createdBy()
                ))
                .flatMap(createUC::execute)
                .map(this::toResponse)
                .flatMap(resp -> ServerResponse.created(
                                req.uriBuilder().path("/{subtypeCode}/{agencyCode}")
                                        .build(resp.subtypeCode(), resp.agencyCode()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> update(ServerRequest req) {
        String subtypeCode = req.pathVariable("subtypeCode");
        String agencyCode  = req.pathVariable("agencyCode");
        return req.bodyToMono(AgencyUpdateRequest.class)
                .flatMap(validation::validate)
                .map(r -> new Agency(
                        subtypeCode, agencyCode, r.name(),
                        r.agencyNit(), r.address(), r.phone(), r.municipalityDaneCode(),
                        r.embosserHighlight(), r.embosserPins(),
                        r.cardCustodianPrimary(), r.cardCustodianPrimaryId(),
                        r.cardCustodianSecondary(), r.cardCustodianSecondaryId(),
                        r.pinCustodianPrimary(), r.pinCustodianPrimaryId(),
                        r.pinCustodianSecondary(), r.pinCustodianSecondaryId(),
                        r.description(), "A", null, null, r.updatedBy() // status se mantiene en repo.save (MERGE)
                ))
                .flatMap(updateUC::execute)
                .map(this::toResponse)
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp));
    }

    public Mono<ServerResponse> changeStatus(ServerRequest req) {
        String subtypeCode = req.pathVariable("subtypeCode");
        String agencyCode  = req.pathVariable("agencyCode");
        return req.bodyToMono(AgencyStatusRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> changeStatusUC.execute(subtypeCode, agencyCode, r.status(), r.updatedBy()))
                .map(this::toResponse)
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp));
    }

    public Mono<ServerResponse> get(ServerRequest req) {
        String subtypeCode = req.pathVariable("subtypeCode");
        String agencyCode  = req.pathVariable("agencyCode");
        return getUC.execute(subtypeCode, agencyCode)
                .map(this::toResponse)
                .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp));
    }

    public Mono<ServerResponse> list(ServerRequest req) {
        String subtype = req.queryParam("subtypeCode").orElse(null);
        String status  = req.queryParam("status").orElse(null);
        String search  = req.queryParam("search").orElse(null);
        int page = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = req.queryParam("size").map(Integer::parseInt).orElse(20);

        Flux<AgencyResponse> body = listUC.execute(subtype, status, search, page, size)
                .map(this::toResponse);

        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(body, AgencyResponse.class);
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

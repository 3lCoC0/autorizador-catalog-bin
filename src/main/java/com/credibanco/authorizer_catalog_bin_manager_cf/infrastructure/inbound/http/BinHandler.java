package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.inbound.http;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.inbound.http.dto.BinCreateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.inbound.http.dto.BinResponse;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public Mono<ServerResponse> create(ServerRequest req) {
    return req.bodyToMono(BinCreateRequest.class)
            .flatMap(r -> createUC.execute(
                    r.bin(), r.name(), r.typeBin(), r.typeAccount(),
                    r.compensationCod(), r.description(), r.createdBy()
            ))
            .map(b -> new BinResponse(
                    b.bin(), b.name(), b.typeBin(), b.typeAccount(),
                    b.compensationCod(), b.description(), b.status(),
                    b.createdAt(), b.updatedAt(), b.updatedBy()
            ))
            .flatMap(resp -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(resp))
            .onErrorResume(e -> ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new ProblemDetails("about:blank","Error creando BIN",400,e.getMessage())));
}
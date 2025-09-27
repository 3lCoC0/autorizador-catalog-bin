package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.http;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.logging.CorrelationWebFilter;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.time.Instant;

public final class ApiResponses {
    private ApiResponses() {}

    public static String cid(ServerRequest req) {
        // El filtro ya lo puso en la respuesta; si no, tomo del request
        String fromResp = req.exchange().getResponse().getHeaders().getFirst(CorrelationWebFilter.CID);
        return (fromResp != null && !fromResp.isBlank())
                ? fromResp
                : req.headers().firstHeader(CorrelationWebFilter.CID);
    }

    public static <T> ApiSuccess<T> okEnvelope(ServerRequest req, String detail, T data) {
        return new ApiSuccess<>("00", detail, cid(req), Instant.now().toString(), data);
    }

    public static ServerResponse.BodyBuilder jsonOk() {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON);
    }

    public static ServerResponse.BodyBuilder jsonCreated(ServerRequest req, String locationBin) {
        return ServerResponse.created(req.uriBuilder().path("/{bin}").build(locationBin))
                .contentType(MediaType.APPLICATION_JSON);
    }
}
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.inbound.http;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.CreateBinUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.ListBinsUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.inbound.http.dto.BinCreateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.inbound.http.dto.BinResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BinHandler {

    private final CreateBinUseCase createUC;
    private final ListBinsUseCase listUC;
    private final ValidationUtil validation;

    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(BinCreateRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> createUC.execute(
                        r.bin(), r.name(), r.typeBin(), r.typeAccount(),
                        r.compensationCod(), r.description(), r.createdBy()
                ))
                .map(b -> new BinResponse(
                        b.bin(), b.name(), b.typeBin(), b.typeAccount(),
                        b.compensationCod(), b.description(), b.status(),
                        b.createdAt(), b.updatedAt(), b.updatedBy()
                ))
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> list(ServerRequest req) {
        int page = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = req.queryParam("size").map(Integer::parseInt).orElse(20);

        Flux<BinResponse> body = listUC.execute(page, size)
                .map(b -> new BinResponse(
                        b.bin(), b.name(), b.typeBin(), b.typeAccount(),
                        b.compensationCod(), b.description(), b.status(),
                        b.createdAt(), b.updatedAt(), b.updatedBy()
                ));

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body, BinResponse.class);
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto.BinStatusUpdateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto.BinUpdateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto.BinCreateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto.BinResponse;
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
    private final UpdateBinUseCase updateUC;
    private final GetBinUseCase getUC;
    private final ChangeBinStatusUseCase changeStatusUC;

    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(BinCreateRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> {

                    if ("Y".equals(r.usesBinExt())) {
                        if (r.binExtDigits() == null || !(r.binExtDigits() == 1 || r.binExtDigits() == 2 || r.binExtDigits() == 3))
                            return Mono.error(new IllegalArgumentException("binExtDigits debe ser 1, 2 o 3 cuando usesBinExt='Y'"));
                    } else {
                        if (r.binExtDigits() != null)
                            return Mono.error(new IllegalArgumentException("binExtDigits debe ser null cuando usesBinExt='N'"));
                    }

                    return createUC.execute(
                            r.bin(), r.name(), r.typeBin(), r.typeAccount(),
                            r.compensationCod(), r.description(),
                            r.usesBinExt(), r.binExtDigits(),
                            r.createdBy() // puede ser null
                    );
                })
                .map(b -> new BinResponse(
                        b.bin(), b.name(), b.typeBin(), b.typeAccount(),
                        b.compensationCod(), b.description(), b.status(),
                        b.createdAt(), b.updatedAt(), b.updatedBy(),
                        b.usesBinExt(), b.binExtDigits()
                ))
                .flatMap(resp -> ServerResponse.created(req.uriBuilder().path("/{bin}").build(resp.bin()))
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
                        b.createdAt(), b.updatedAt(), b.updatedBy(),
                        b.usesBinExt(), b.binExtDigits()
                ));

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body, BinResponse.class);
    }


    public Mono<ServerResponse> update(ServerRequest req) {
        return req.bodyToMono(BinUpdateRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> {
                    if ("Y".equals(r.usesBinExt())) {
                        if (r.binExtDigits() == null || !(r.binExtDigits() == 1 || r.binExtDigits() == 2 || r.binExtDigits() == 3))
                            return Mono.error(new IllegalArgumentException("binExtDigits debe ser 1, 2 o 3 cuando usesBinExt='Y'"));
                    } else {
                        if (r.binExtDigits() != null)
                            return Mono.error(new IllegalArgumentException("binExtDigits debe ser null cuando usesBinExt='N'"));
                    }

                    return updateUC.execute(
                            r.bin(), r.name(), r.typeBin(), r.typeAccount(),
                            r.compensationCod(), r.description(),
                            r.usesBinExt(), r.binExtDigits(),
                            r.updatedBy() // puede ser null
                    );
                })
                .map(b -> new BinResponse(
                        b.bin(), b.name(), b.typeBin(), b.typeAccount(),
                        b.compensationCod(), b.description(), b.status(),
                        b.createdAt(), b.updatedAt(), b.updatedBy(),
                        b.usesBinExt(), b.binExtDigits()
                ))
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

    public Mono<ServerResponse> get(ServerRequest req) {
        String bin = req.pathVariable("bin");
        return getUC.execute(bin)
                .map(b -> new BinResponse(
                        b.bin(), b.name(), b.typeBin(), b.typeAccount(),
                        b.compensationCod(), b.description(), b.status(),
                        b.createdAt(), b.updatedAt(), b.updatedBy(),
                        b.usesBinExt(), b.binExtDigits()
                ))
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }


    public Mono<ServerResponse> changeStatus(ServerRequest req) {
        String bin = req.pathVariable("bin");
        return req.bodyToMono(BinStatusUpdateRequest.class)
                .flatMap(validation::validate)
                .flatMap(r -> changeStatusUC.execute(bin, r.status(), r.updatedBy()))
                .map(b -> new BinResponse(
                        b.bin(), b.name(), b.typeBin(), b.typeAccount(),
                        b.compensationCod(), b.description(), b.status(),
                        b.createdAt(), b.updatedAt(), b.updatedBy(),
                        b.usesBinExt(), b.binExtDigits()
                ))
                .flatMap(resp -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(resp));
    }

}

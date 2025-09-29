package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.http.ApiResponses;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.http.ApiResponses.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class BinHandler {

    private final CreateBinUseCase createUC;
    private final ListBinsUseCase listUC;
    private final ValidationUtil validation;
    private final UpdateBinUseCase updateUC;
    private final GetBinUseCase getUC;
    private final ChangeBinStatusUseCase changeStatusUC;


    private static long elapsedMs(long t0) { return (System.nanoTime() - t0) / 1_000_000; }

    private static Mono<Void> checkExtConstraints(String bin, String usesExt, Integer extDigits) {
        boolean uses = "Y".equalsIgnoreCase(usesExt);

        if (uses) {
            if (extDigits == null || (extDigits != 1 && extDigits != 2 && extDigits != 3)) {
                return Mono.error(new AppException(
                        AppError.BIN_EXT_DIGITS_INVALID,
                        "binExtDigits debe ser 1, 2 o 3 cuando usesBinExt='Y'"
                ));
            }
            int baseLen = (bin == null) ? 0 : bin.trim().length();
            if (baseLen + extDigits > 9) {
                return Mono.error(new AppException(
                        AppError.BIN_EXT_TOTAL_LENGTH_OVERFLOW,
                        "bin + extensión no puede exceder 9 dígitos"
                ));
            }
        } else {
            if (extDigits != null) {
                return Mono.error(new AppException(
                        AppError.BIN_EXT_DIGITS_MUST_BE_NULL_ON_N,
                        "binExtDigits debe ser null cuando usesBinExt='N'"
                ));
            }
        }
        return Mono.empty();
    }


    private BinResponse toResponse(com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin b) {
        return new BinResponse(
                b.bin(), b.name(), b.typeBin(), b.typeAccount(),
                b.compensationCod(), b.description(), b.status(),
                b.createdAt(), b.updatedAt(), b.updatedBy(),
                b.usesBinExt(), b.binExtDigits()
        );
    }

    public Mono<ServerResponse> create(ServerRequest req) {
        long t0 = System.nanoTime();
        return req.bodyToMono(BinCreateRequest.class)
                .flatMap(r -> validation.validate(r, AppError.BIN_INVALID_DATA)) // "01"
                .flatMap(r -> checkExtConstraints(r.bin(), r.usesBinExt(), r.binExtDigits())
                        .onErrorMap(IllegalArgumentException.class,
                                e -> new AppException(AppError.BIN_INVALID_DATA, e.getMessage()))
                        .then(createUC.execute(
                                r.bin(), r.name(), r.typeBin(), r.typeAccount(),
                                r.compensationCod(), r.description(),
                                r.usesBinExt(), r.binExtDigits(),
                                r.createdBy())))
                .doOnSuccess(b -> log.info("BIN:create:done bin={}, status={}, elapsedMs={}",
                        b.bin(), b.status(), elapsedMs(t0)))
                .map(this::toResponse)
                .flatMap(body -> ApiResponses.jsonCreated(req, body.bin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ApiResponses.okEnvelope(req, "Bin se creo exitosamente", body)));
    }


    public Mono<ServerResponse> list(ServerRequest req) {
        long t0 = System.nanoTime();
        int page = req.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = req.queryParam("size").map(Integer::parseInt).orElse(20);
        log.info("BIN:list:recv page={}, size={}", page, size);
        return listUC.execute(page, size)
                .map(this::toResponse)
                .collectList()
                .doOnSuccess(list -> log.info("BIN:list:done page={} size={} count={} elapsedMs={}",
                        page, size, list.size(), elapsedMs(t0)))
                .flatMap(list -> jsonOk().bodyValue(okEnvelope(req, "Operación exitosa", list)));
    }

    public Mono<ServerResponse> update(ServerRequest req) {
        long t0 = System.nanoTime();
        return req.bodyToMono(BinUpdateRequest.class)
                .doOnSubscribe(s -> log.info("BIN:update:recv"))
                .flatMap(r -> validation.validate(r, AppError.BIN_INVALID_DATA))
                .flatMap(r -> {
                    log.debug("BIN:update:validated bin={}, usesExt={}, extDigits={}",
                            r.bin(), r.usesBinExt(), r.binExtDigits());

                    String by = resolveUser(req, r.updatedBy());
                    return checkExtConstraints(r.bin(), r.usesBinExt(), r.binExtDigits())
                            .onErrorMap(IllegalArgumentException.class,
                                    e -> new AppException(AppError.BIN_INVALID_DATA, e.getMessage()))
                            .then(updateUC.execute(
                                    r.bin(), r.name(), r.typeBin(), r.typeAccount(),
                                    r.compensationCod(), r.description(),
                                    r.usesBinExt(), r.binExtDigits(),
                                    by
                            ));
                })
                .doOnSuccess(b -> log.info("BIN:update:done bin={}, status={}, elapsedMs={}",
                        b.bin(), b.status(), elapsedMs(t0)))
                .map(this::toResponse)
                .flatMap(body -> ApiResponses.jsonOk()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ApiResponses.okEnvelope(req, "Operación exitosa", body)));
    }

    public Mono<ServerResponse> get(ServerRequest req) {
        long t0 = System.nanoTime();
        String bin = req.pathVariable("bin");
        return getUC.execute(bin)
                .doOnSubscribe(s -> log.info("BIN:get:recv bin={}", bin))
                .doOnSuccess(b -> log.info("BIN:get:done bin={}, status={}, elapsedMs={}",
                        b.bin(), b.status(), elapsedMs(t0)))
                .map(this::toResponse)
                .flatMap(body -> jsonOk().bodyValue(okEnvelope(req, "Operación exitosa", body)));
    }


    public Mono<ServerResponse> changeStatus(ServerRequest req) {
        long t0 = System.nanoTime();
        String bin = req.pathVariable("bin");
        return req.bodyToMono(BinStatusUpdateRequest.class)
                .doOnSubscribe(s -> log.info("BIN:status:recv bin={}", bin))
                .flatMap(r -> validation.validate(r, AppError.BIN_INVALID_DATA))
                .flatMap(r -> changeStatusUC.execute(bin, r.status(), r.updatedBy()))
                .doOnSuccess(b -> log.info("BIN:status:done bin={}, newStatus={}, elapsedMs={}",
                        b.bin(), b.status(), elapsedMs(t0)))
                .map(this::toResponse)
                .flatMap(body -> jsonOk()
                        .bodyValue(okEnvelope(req, "Se cambio el STATUS del bin correctamente", body)));
    }


    private String resolveUser(ServerRequest req, String fromBody) {
        String hdr = req.headers().firstHeader("X-User");
        return StringUtils.hasText(fromBody) ? fromBody
                : (StringUtils.hasText(hdr) ? hdr : null); // opcional
    }
}

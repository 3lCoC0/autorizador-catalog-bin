package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.http.ApiError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.http.ApiResponses;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.QueryTimeoutException;
import org.springframework.dao.CannotAcquireLockException;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Handler global de errores.
 *
 * Responde SIEMPRE con envelope JSON:
 * {
 *   "responseCode": "<num>",
 *   "message": "<detalle legible>",
 *   "correlationId": "<cid>",
 *   "timestamp": "<UTC ISO-8601>",
 *   "path": "<ruta>"
 * }
 *
 * - Si la excepción es AppException, usa el AppError provisto (código numérico y HTTP status).
 * - Si no, aplica mapeos de fallback mientras migramos a AppException por dominio.
 */
@Component
@Order(-2)
@Slf4j
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private static final String HDR_CID = "X-Correlation-Id";
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public @NonNull Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        var response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // --- Datos de request/response base
        var request = exchange.getRequest();
        String path = request.getPath().pathWithinApplication().value();

        String cid = request.getHeaders().getFirst(HDR_CID);
        if (!StringUtils.hasText(cid)) cid = UUID.randomUUID().toString();
        response.getHeaders().set(HDR_CID, cid);

        // --- Mapeo principal a AppError + mensaje
        AppError appError;
        String message;

        if (ex instanceof AppException ae) {
            // Camino "ideal": los casos de uso lanzan AppException con su AppError tipado
            appError = ae.getError();
            message = safeMessage(ae.getMessage(), appError.defaultMessage);

        } else if (ex instanceof ServerWebInputException
                || ex instanceof DecodingException
                || ex instanceof ConstraintViolationException
                || ex instanceof IllegalArgumentException) {
            // Errores de validación/deserialización
            var mapped = mapFallbackValidation(path);
            appError = mapped.error;
            message  = safeMessage(unwrapMessage(ex), mapped.message);

        } else if (ex instanceof ResponseStatusException rse) {
            // Respetamos el status si viene de RSE, pero con nuestros códigos
            HttpStatus st = HttpStatus.resolve(rse.getStatusCode().value());
            if (st == null) st = HttpStatus.INTERNAL_SERVER_ERROR;
            var mapped = mapFallbackByStatus(path, st);
            appError = mapped.error;
            message = safeMessage(rse.getReason(), mapped.message);



        } else if (ex instanceof java.util.NoSuchElementException || ex instanceof NotFoundException) {
            // No encontrado
            var mapped = mapFallbackNotFound(path);
            appError = mapped.error;
            message  = safeMessage(unwrapMessage(ex), mapped.message);
        } else if (ex instanceof IllegalStateException) {
            // Conflicto de negocio (duplicados, reglas)
            var mapped = mapFallbackConflict(path);
            appError = mapped.error;
            message  = safeMessage(unwrapMessage(ex), mapped.message);
        } else if (isTimeout(ex)) {
            // Timeouts técnicos
            appError = AppError.INTERNAL;
            message  = "La operación excedió el tiempo de espera.";
        } else {
            // Desconocido → 500 con código 99
            appError = AppError.INTERNAL;
            message  = "Se produjo un error inesperado.";
        }

        // --- Logging
        if (appError.http.is5xxServerError()) {
            log.error("Unhandled error | httpStatus={} code={} cid={} path={} msg={}",
                    appError.http.value(), appError.code, cid, path, ex.getMessage(), ex);
        } else {
            log.warn("Client/expected error | httpStatus={} code={} cid={} path={} msg={}",
                    appError.http.value(), appError.code, cid, path, message);
        }

        // --- Construcción del envelope y escritura de respuesta
        ApiError body = ApiResponses.errorEnvelope(appError.code, message, cid, path);

        response.setStatusCode(appError.http);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] json = mapper.writeValueAsBytes(body);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(json)));
        } catch (Exception writeEx) {
            // Fallback ultraseguro
            String fallback = """
                {"responseCode":"%s","message":"%s","correlationId":"%s","timestamp":"%s","path":"%s"}
                """.formatted(appError.code, message, cid, Instant.now(), path);
            var buf = response.bufferFactory().wrap(fallback.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buf));
        }
    }

    // ========================================================================
    // Fallbacks por dominio (mientras migramos throws a AppException)
    // ========================================================================

    /**
     * Determina si la ruta pertenece al dominio BIN.
     * Ajusta aquí si tus rutas cambian.
     */
    private boolean isBinPath(String path) {
        return path != null && path.startsWith("/bins/");
    }

    private MappedError mapFallbackValidation(String path) {
        if (isBinPath(path)) {
            return new MappedError(AppError.BIN_INVALID_DATA, AppError.BIN_INVALID_DATA.defaultMessage);
        }
        return new MappedError(AppError.INTERNAL, "Solicitud inválida");
    }

    private MappedError mapFallbackNotFound(String path) {
        if (isBinPath(path)) {
            return new MappedError(AppError.BIN_NOT_FOUND, AppError.BIN_NOT_FOUND.defaultMessage);
        }
        return new MappedError(AppError.INTERNAL, "Recurso no encontrado");
    }

    private MappedError mapFallbackConflict(String path) {
        if (isBinPath(path)) {
            return new MappedError(AppError.BIN_ALREADY_EXISTS, AppError.BIN_ALREADY_EXISTS.defaultMessage);
        }
        return new MappedError(AppError.INTERNAL, "Conflicto de negocio");
    }

    private MappedError mapFallbackByStatus(String path, HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> mapFallbackValidation(path);
            case NOT_FOUND   -> mapFallbackNotFound(path);
            case CONFLICT    -> mapFallbackConflict(path);
            default          -> new MappedError(AppError.INTERNAL, "Se produjo un error inesperado");
        };
    }

    // ========================================================================
    // Utilidades
    // ========================================================================

    /** Retorna la causa más específica (última) de la cadena de excepciones. */
    private static Throwable mostSpecific(Throwable ex) {
        Throwable cur = ex;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur;
    }

    /** Intenta extraer el mensaje “original” de las excepciones de Jackson. */
    private static String jacksonMessageOrNull(Throwable t) {
        if (t instanceof MismatchedInputException mie) {
            String m = mie.getOriginalMessage();
            return (m != null && !m.isBlank()) ? m.trim() : trimOrNull(mie.getMessage());
        }
        if (t instanceof InvalidFormatException ife) {
            String m = ife.getOriginalMessage();
            return (m != null && !m.isBlank()) ? m.trim() : trimOrNull(ife.getMessage());
        }
        if (t instanceof ValueInstantiationException vie) {
            String m = vie.getOriginalMessage();
            return (m != null && !m.isBlank()) ? m.trim() : trimOrNull(vie.getMessage());
        }
        return null;
    }

    private static String trimOrNull(String s) {
        return (s == null) ? null : s.trim();
    }


    private static String safeMessage(String given, String fallback) {
        if (StringUtils.hasText(given)) return truncate(given.trim(), 500);
        return fallback;
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }

    private String unwrapMessage(Throwable ex) {


        // 1) Si es DecodingException o ServerWebInputException, intenta sacar el mensaje de Jackson
        if (ex instanceof DecodingException || ex instanceof ServerWebInputException) {
            // Primero, busca la causa más específica
            Throwable root = mostSpecific(ex);
            // Intenta extraer el “originalMessage” de Jackson
            String jm = jacksonMessageOrNull(root);
            if (StringUtils.hasText(jm)) return truncate(jm, 500);
            // A veces la causa directa útil está un nivel arriba
            if (ex.getCause() != null) {
                jm = jacksonMessageOrNull(ex.getCause());
                if (StringUtils.hasText(jm)) return truncate(jm, 500);
            }
        }


        // Busca un mensaje razonable en la cadena de causas (IllegalArgumentException, etc.)
        Throwable t = ex;
        while (t != null) {
            if (t instanceof IllegalArgumentException && StringUtils.hasText(t.getMessage())) {
                return t.getMessage();
            }
            if (t instanceof ResponseStatusException rse && StringUtils.hasText(rse.getReason())) {
                return rse.getReason();
            }
            t = t.getCause();
        }
        return null; // lo rellenará safeMessage(...)
    }

    private boolean isTimeout(Throwable ex) {
        return findCause(ex, TimeoutException.class) != null
                || findCause(ex, QueryTimeoutException.class) != null
                || findCause(ex, LockTimeoutException.class) != null
                || findCause(ex, org.springframework.dao.QueryTimeoutException.class) != null
                || findCause(ex, CannotAcquireLockException.class) != null
                || findCause(ex, SocketTimeoutException.class) != null
                || findCause(ex, ReadTimeoutException.class) != null
                || findCause(ex, ConnectTimeoutException.class) != null;
    }

    private static <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
        Throwable t = ex;
        while (t != null) {
            if (type.isInstance(t)) return type.cast(t);
            t = t.getCause();
        }
        return null;
    }

     // ========================================================================
    // Tipos auxiliares
    // ========================================================================

    private record MappedError(AppError error, String message) {}

    /**
     * 404 específico si lo necesitas en tu dominio/aplicación.
     */
    public static final class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }
}

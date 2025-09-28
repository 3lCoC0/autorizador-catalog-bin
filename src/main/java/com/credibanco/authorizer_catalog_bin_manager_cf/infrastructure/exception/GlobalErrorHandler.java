package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import io.r2dbc.spi.R2dbcTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.channel.ConnectTimeoutException;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2)
@Slf4j
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public @NonNull Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        var response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        var req = exchange.getRequest();

        String cid = req.getHeaders().getFirst("X-Correlation-Id");
        if (!StringUtils.hasText(cid)) cid = UUID.randomUUID().toString();
        response.getHeaders().set("X-Correlation-Id", cid);

        String instance = req.getPath().pathWithinApplication().value();
        String title = status.getReasonPhrase();
        String detail = buildDetail(ex, status);
        String type = problemTypeFor(status);

        // Logging único por error
        if (status.is5xxServerError()) {
            log.error("Unhandled error | status={} cid={} path={} msg={}",
                    status.value(), cid, instance, ex.getMessage(), ex);
        } else {
            log.warn("Client/expected error | status={} cid={} path={} msg={}",
                    status.value(), cid, instance, detail);
        }

        Map<String, Object> body = Map.of(
                "type", type,
                "title", title,
                "status", status.value(),
                "detail", detail,
                "instance", instance,
                "timestamp", Instant.now().toString(),
                "correlationId", cid
        );

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] json = mapper.writeValueAsBytes(body);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(json)));
        } catch (Exception writeEx) {
            var fallback = """
                    {"type":"about:blank","title":"%s","status":%d,"detail":"%s","instance":"%s","correlationId":"%s"}
                    """.formatted(title, status.value(), detail, instance, cid);
            var buf = response.bufferFactory().wrap(fallback.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buf));
        }
    }

    // --- helpers ---

    private boolean isTimeout(Throwable ex) {
        return findCause(ex, TimeoutException.class) != null
                || findCause(ex, R2dbcTimeoutException.class) != null
                || findCause(ex, SocketTimeoutException.class) != null
                || findCause(ex, ReadTimeoutException.class) != null
                || findCause(ex, ConnectTimeoutException.class) != null;
    }

    private Throwable firstTimeoutCause(Throwable ex) {
        Throwable t;
        if ((t = findCause(ex, TimeoutException.class)) != null) return t;
        if ((t = findCause(ex, R2dbcTimeoutException.class)) != null) return t;
        if ((t = findCause(ex, SocketTimeoutException.class)) != null) return t;
        if ((t = findCause(ex, ReadTimeoutException.class)) != null) return t;
        if ((t = findCause(ex, ConnectTimeoutException.class)) != null) return t;
        return ex;
    }

    private boolean messageContains(Throwable ex, String token) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            String m = t.getMessage();
            if (m != null && m.contains(token)) return true;
        }
        return false;
    }

    // --- mapeo de status ---

    private HttpStatus resolveStatus(Throwable ex) {

        if (ex instanceof ResponseStatusException rse) {
            var sc = rse.getStatusCode();
            var status = HttpStatus.resolve(sc.value());
            return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (ex instanceof IllegalArgumentException || ex instanceof ConstraintViolationException || ex instanceof DecodingException) {
            return HttpStatus.BAD_REQUEST;
        }

        if (ex instanceof NotFoundException || ex instanceof NoSuchElementException) return HttpStatus.NOT_FOUND;

        // Regla de dominio desde BD: no permitir inactivar la última agency activa
        if (messageContains(ex, "ORA-20052")) return HttpStatus.CONFLICT;

        if (ex instanceof IllegalStateException) return HttpStatus.CONFLICT;

        // Timeouts (R2DBC/Netty/Reactor)
        if (isTimeout(ex)) return HttpStatus.GATEWAY_TIMEOUT;

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    // --- detalle del problema ---

    private String buildDetail(Throwable ex, HttpStatus status) {

        // 1) Errores de lectura/binding del cuerpo
        if (ex instanceof ServerWebInputException || ex instanceof DecodingException) {
            Throwable cause = (ex instanceof ServerWebInputException) ? ex.getCause() : ex;

            var iae = findCause(cause, IllegalArgumentException.class);
            if (iae != null && StringUtils.hasText(iae.getMessage())) {
                return iae.getMessage();
            }

            var vie = findCause(cause, ValueInstantiationException.class);
            if (vie != null) {
                var inner = findCause(vie, IllegalArgumentException.class);
                if (inner != null && StringUtils.hasText(inner.getMessage())) {
                    return inner.getMessage();
                }
            }

            var ife = findCause(cause, InvalidFormatException.class);
            if (ife != null) {
                Class<?> t = ife.getTargetType();
                if (t == com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType.class) {
                    return "dataType inválido: '" + ife.getValue()
                            + "'. Valores permitidos: "
                            + com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType.ALLOWED;
                }
                if (t != null && t.isEnum()) {
                    return "Valor inválido: '" + ife.getValue() + "' para " + t.getSimpleName();
                }
            }

            return "Cuerpo JSON inválido o con tipos incorrectos";
        }

        // 2) Para otras ResponseStatusException, respeta su reason
        if (ex instanceof ResponseStatusException rse
                && StringUtils.hasText(rse.getReason())) {
            return rse.getReason();
        }

        // 3) Detalle específico para 504
        if (status == HttpStatus.GATEWAY_TIMEOUT) {
            Throwable root = firstTimeoutCause(ex);
            String msg = (root.getMessage() != null) ? root.getMessage() : "";
            if (msg.isBlank()) msg = "La operación excedió el tiempo de espera.";
            if (msg.length() > 300) msg = msg.substring(0, 300) + "...";

            if (msg.contains("Connection acquisition")) {
                msg = "Tiempo de espera al adquirir conexión a la base de datos.";
            } else if (msg.contains("Did not observe any item or terminal signal")) {
                msg = "Tiempo de espera en operación reactiva (no hubo señal dentro del límite).";
            }
            return msg;
        }

        // 4) Mensaje claro para la validación de dominio desde BD (trigger)
        if (messageContains(ex, "ORA-20052")) {
            return "No puede inactivarse la única AGENCY activa del SUBTYPE. " +
                    "Cree otra AGENCY activa o inhabilite el SUBTYPE.";
        }

        // 5) Resto
        String msg = ex.getMessage();
        if (!StringUtils.hasText(msg)) {
            return status.is5xxServerError() ? "Se produjo un error inesperado." : "Solicitud inválida.";
        }
        return status.is5xxServerError() ? "Se produjo un error inesperado." : msg;
    }

    private String problemTypeFor(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "https://credibanco.com/problems/validation-error";
            case NOT_FOUND -> "https://credibanco.com/problems/not-found";
            case CONFLICT -> "https://credibanco.com/problems/conflict";
            case GATEWAY_TIMEOUT -> "https://credibanco.com/problems/gateway-timeout";
            default -> "https://credibanco.com/problems/internal-error";
        };
    }

    // Custom 404 si la necesitas en tu dominio/aplicación
    public static final class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }

    private static <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
        Throwable t = ex;
        while (t != null) {
            if (type.isInstance(t)) return type.cast(t);
            t = t.getCause();
        }
        return null;
    }
}

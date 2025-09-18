package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import reactor.core.publisher.Mono;

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

    private HttpStatus resolveStatus(Throwable ex) {
        // Respeta el status si viene envuelto por Spring
        if (ex instanceof ResponseStatusException rse) {
            var sc = rse.getStatusCode();
            var status = HttpStatus.resolve(sc.value());
            return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // 400 - validación / parseo
        if (ex instanceof IllegalArgumentException ||
                ex instanceof ConstraintViolationException ||
                ex instanceof DecodingException) {
            return HttpStatus.BAD_REQUEST;
        }
        // 404 - no encontrado (custom o mapeo simple)
        if (ex instanceof NotFoundException || ex instanceof NoSuchElementException) {
            return HttpStatus.NOT_FOUND;
        }
        // 409 - conflicto de estado de negocio
        if (ex instanceof IllegalStateException) {
            return HttpStatus.CONFLICT;
        }
        // 504 - timeout genérico
        if (ex instanceof TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        // 500 - resto
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String buildDetail(Throwable ex, HttpStatus status) {
        if (ex instanceof ResponseStatusException rse && StringUtils.hasText(rse.getReason())) {
            return rse.getReason();
        }
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
}

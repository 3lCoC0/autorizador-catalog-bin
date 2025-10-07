package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationWebFilter implements WebFilter {

    public static final String CID = "X-Correlation-Id";
    public static final String CTX_CID = "ctx.correlationId";
    public static final String CTX_USER = "ctx.userId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 1) Obtener/crear correlation id y user desde headers
        String cid = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(CID))
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());
        String user = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-User"))
                .orElse(null);

        if (log.isInfoEnabled()) {
            log.info("correlation filter - IN method={} path={} cid={} headerUser={}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath().value(),
                    cid,
                    (user == null || user.isBlank()) ? "<none>" : user);
        }

        // 2) Propagar header de salida
        exchange.getResponse().getHeaders().set(CID, cid);

        // 3) Ejecutar la cadena con contexto reactor y MDC en cada señal
        return chain.filter(exchange)
                // En cada señal copiamos desde Context -> MDC
                .doOnEach(signal -> {
                    ContextView ctx = signal.getContextView();
                    String ctxCid = ctx.getOrDefault(CTX_CID, cid);
                    String ctxUser = ctx.getOrDefault(CTX_USER, user);
                    if (ctxCid != null) MDC.put("cid", ctxCid);
                    if (ctxUser != null) MDC.put("user", ctxUser);
                })
                // Al finalizar, limpiamos MDC
                .doFinally(st -> {
                    MDC.remove("cid");
                    MDC.remove("user");
                })
                // 4) Escribimos cid/user al Context para que esté disponible río abajo
                .contextWrite(ctx -> {
                    var withCid = ctx.put(CTX_CID, cid);
                    return (user != null) ? withCid.put(CTX_USER, user) : withCid;
                });
    }
}

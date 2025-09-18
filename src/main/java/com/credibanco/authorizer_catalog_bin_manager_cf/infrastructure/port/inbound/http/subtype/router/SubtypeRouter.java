package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.router;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.handler.SubtypeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

public class SubtypeRouter {
    @Bean
    public RouterFunction<ServerResponse> subtypeRoutes(SubtypeHandler h) {
        return RouterFunctions.route()
                .path("/subtypes", builder -> builder
                        .POST("", h::create)
                        .GET("", h::list)
                        .GET("/{bin}/{code}", h::get)
                        .PUT("/{bin}/{code}", h::update)
                        .PATCH("/{bin}/{code}/status", h::changeStatus)
                        .DELETE("/{bin}/{code}", h::delete)
                )
                .build();
    }
}
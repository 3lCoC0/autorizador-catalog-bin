package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.router;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.handler.SubtypeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration
@RequiredArgsConstructor
public class SubtypeRouter {

    private final SubtypeHandler handler;

    @Bean("subtypeRoutes")
    RouterFunction<ServerResponse> subtypeRoutes() {
        return RouterFunctions.route()
                .POST("/v1/subtypes", accept(MediaType.APPLICATION_JSON), handler::create)
                .GET ("/v1/subtypes", accept(MediaType.APPLICATION_JSON), handler::list)
                .GET ("/v1/subtypes/{bin}/{code}", accept(MediaType.APPLICATION_JSON), handler::get)
                .PUT ("/v1/subtypes/{bin}/{code}", accept(MediaType.APPLICATION_JSON), handler::update)
                .PUT ("/v1/subtypes/{bin}/{code}/status", accept(MediaType.APPLICATION_JSON), handler::changeStatus)
                .build();
    }
}

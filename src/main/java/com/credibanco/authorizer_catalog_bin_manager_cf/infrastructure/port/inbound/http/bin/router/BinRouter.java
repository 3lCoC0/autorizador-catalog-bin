package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.router;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.handler.BinHandler;
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
public class BinRouter {
    private final BinHandler handler;

    @Bean
    RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .POST("/v1/bins", accept(MediaType.APPLICATION_JSON), handler::create)
                .GET("/v1/bins",  accept(MediaType.APPLICATION_JSON), handler::list)
                .GET("/v1/bins/{bin}",  accept(MediaType.APPLICATION_JSON), handler::get)
                .PUT("/v1/bins/{bin}",(accept(MediaType.APPLICATION_JSON)), handler::update)
                .PUT ("/v1/bins/{bin}/status",     accept(MediaType.APPLICATION_JSON), handler::changeStatus)
                .build();
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.router;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.handler.AgencyHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration
@RequiredArgsConstructor
public class AgencyRouter {
    private final AgencyHandler handler;

    @Bean("agencyRoutes")
    RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .POST("/v1/agencies", accept(MediaType.APPLICATION_JSON), handler::create)
                .GET ("/v1/agencies",  accept(MediaType.APPLICATION_JSON), handler::list)
                .GET ("/v1/agencies/{subtypeCode}/{agencyCode}", accept(MediaType.APPLICATION_JSON), handler::get)
                .PUT ("/v1/agencies/{subtypeCode}/{agencyCode}", accept(MediaType.APPLICATION_JSON), handler::update)
                .PUT ("/v1/agencies/{subtypeCode}/{agencyCode}/status", accept(MediaType.APPLICATION_JSON), handler::changeStatus)
                .build();
    }
}

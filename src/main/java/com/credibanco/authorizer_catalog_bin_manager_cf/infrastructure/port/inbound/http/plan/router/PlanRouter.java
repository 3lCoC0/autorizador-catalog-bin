package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.router;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.handler.PlanHandler;
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
public class PlanRouter {

    private final PlanHandler handler;

    @Bean("planRoutes")
    RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                // CRUD Plan
                .POST("/v1/plans", accept(MediaType.APPLICATION_JSON), handler::create)
                .GET ("/v1/plans",  accept(MediaType.APPLICATION_JSON), handler::list)
                .GET ("/v1/plans/{code}", accept(MediaType.APPLICATION_JSON), handler::get)
                .PUT ("/v1/plans/status", accept(MediaType.APPLICATION_JSON), handler::changeStatus)
                .PUT ("/v1/plans/{code}", accept(MediaType.APPLICATION_JSON), handler::update)



                .POST("/v1/plans/items", accept(MediaType.APPLICATION_JSON), handler::addItem)

                .GET ("/v1/plans/{code}/items", accept(MediaType.APPLICATION_JSON), handler::listItems)

                .PUT ("/v1/plans/items/status", accept(MediaType.APPLICATION_JSON), handler::changeItemStatus)


                .POST("/v1/plans/assign", accept(MediaType.APPLICATION_JSON), handler::assignToSubtype)

                .build();
    }
}

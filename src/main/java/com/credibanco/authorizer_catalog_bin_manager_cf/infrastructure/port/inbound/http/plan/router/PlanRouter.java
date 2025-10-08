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
                .POST("/plans/create", accept(MediaType.APPLICATION_JSON), handler::create)
                .GET ("/plans/list",  accept(MediaType.APPLICATION_JSON), handler::list)
                .GET ("/plans/get/{code}", accept(MediaType.APPLICATION_JSON), handler::get)
                .PUT ("/plans/update/status", accept(MediaType.APPLICATION_JSON), handler::changeStatus)
                .PUT ("/plans/update/{code}", accept(MediaType.APPLICATION_JSON), handler::update)



                .POST("/plans/items/attach", accept(MediaType.APPLICATION_JSON), handler::addItem)

                .GET ("/plans/items/get/{planCode}", accept(MediaType.APPLICATION_JSON), handler::listItems)

                .PUT ("/plans/items/update/status", accept(MediaType.APPLICATION_JSON), handler::changeItemStatus)


                .POST("/plans/assign/subtype", accept(MediaType.APPLICATION_JSON), handler::assignToSubtype)

                .build();
    }
}

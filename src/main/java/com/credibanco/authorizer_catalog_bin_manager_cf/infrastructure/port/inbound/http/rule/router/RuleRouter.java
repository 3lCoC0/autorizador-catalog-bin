package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.router;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.handler.RuleHandler;
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
public class RuleRouter {
    private final RuleHandler h;

    @Bean("ruleRoutes")
    RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                // catálogo de validaciones
                .POST("/v1/validations", accept(MediaType.APPLICATION_JSON), h::createValidation)
                .GET ("/v1/validations",  accept(MediaType.APPLICATION_JSON), h::listValidations)
                .GET ("/v1/validations/{code}", accept(MediaType.APPLICATION_JSON), h::getValidation)
                .PUT ("/v1/validations/{code}", accept(MediaType.APPLICATION_JSON), h::updateValidation)
                .PUT ("/v1/validations/{code}/status", accept(MediaType.APPLICATION_JSON), h::changeValidationStatus)
                // mapeos por subtype + bin_efectivo
                .POST("/v1/subtypes/{subtypeCode}/bins/{binEfectivo}/rules/{code}", accept(MediaType.APPLICATION_JSON), h::attachRule)
                .PUT ("/v1/subtypes/{subtypeCode}/bins/{binEfectivo}/rules/{code}/status", accept(MediaType.APPLICATION_JSON), h::changeRuleStatus)
                .GET ("/v1/subtypes/{subtypeCode}/bins/{binEfectivo}/rules", accept(MediaType.APPLICATION_JSON), h::listRulesForSubtype)
                .build();
    }
}

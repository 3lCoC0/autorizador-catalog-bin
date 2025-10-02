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

                .POST("/validations/create", accept(MediaType.APPLICATION_JSON), h::createValidation)
                .GET ("/validations/list",  accept(MediaType.APPLICATION_JSON), h::listValidations)
                .GET ("/validations/get/{code}", accept(MediaType.APPLICATION_JSON), h::getValidation)
                .PUT ("/validations/update/{code}", accept(MediaType.APPLICATION_JSON), h::updateValidation)
                .PUT ("/validations/update/status/{code}", accept(MediaType.APPLICATION_JSON), h::changeValidationStatus)

                .POST("/validations/attach", accept(MediaType.APPLICATION_JSON), h::attachRule)

                .PUT ("/validations/subtypes/{subtypeCode}/bins/{bin}/rules/status/{code}", accept(MediaType.APPLICATION_JSON), h::changeRuleStatus)

                .GET ("/validations/subtypes/list/{subtypeCode}", accept(MediaType.APPLICATION_JSON), h::listRulesForSubtypeBySubtype)


                .GET ("/v1/subtypes/{subtypeCode}/bins/{bin}/rules", accept(MediaType.APPLICATION_JSON), h::listRulesForSubtype)
                .GET ("/v1/subtypes/{subtypeCode}/rules", accept(MediaType.APPLICATION_JSON), h::listRulesForSubtypeBySubtype)

                .build();
    }
}

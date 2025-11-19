package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security.ActorProvider;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.router.RuleRouter;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RuleHandlerTest {

    private CreateValidationUseCase createUC;
    private UpdateValidationUseCase updateUC;
    private ChangeValidationStatusUseCase changeStatusUC;
    private GetValidationUseCase getUC;
    private ListValidationsUseCase listUC;
    private MapRuleUseCase mapRuleUC;
    private ListRulesForSubtypeUseCase listRulesUC;
    private ValidationUtil validationUtil;
    private ActorProvider actorProvider;
    private WebTestClient client;

    @BeforeEach
    void setup() {
        createUC = mock(CreateValidationUseCase.class);
        updateUC = mock(UpdateValidationUseCase.class);
        changeStatusUC = mock(ChangeValidationStatusUseCase.class);
        getUC = mock(GetValidationUseCase.class);
        listUC = mock(ListValidationsUseCase.class);
        mapRuleUC = mock(MapRuleUseCase.class);
        listRulesUC = mock(ListRulesForSubtypeUseCase.class);
        validationUtil = mock(ValidationUtil.class);
        actorProvider = mock(ActorProvider.class);

        RuleHandler handler = new RuleHandler(createUC, updateUC, changeStatusUC, getUC, listUC, mapRuleUC, listRulesUC,
                validationUtil, actorProvider);
        RuleRouter router = new RuleRouter(handler);
        try {
            var method = RuleRouter.class.getDeclaredMethod("routes");
            method.setAccessible(true);
            client = WebTestClient.bindToRouterFunction((RouterFunction<ServerResponse>) method.invoke(router))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(validationUtil.validate(any(), any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(actorProvider.currentUserId()).thenReturn(Mono.just("secUser"));
    }

    @Test
    void createValidationEndpointReturnsCreatedAggregate() {
        Validation aggregate = Validation.createNew("CODE", "DESC", ValidationDataType.TEXT, "creator");
        when(createUC.execute(anyString(), anyString(), any(), any())).thenReturn(Mono.just(aggregate));

        ValidationCreateRequest request = new ValidationCreateRequest("CODE", "DESC", "TEXT", null, null, null, "creator");

        client.post().uri("/validations/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.code").isEqualTo("CODE");
    }

    @Test
    void updateValidationEndpointUsesPathVariable() {
        Validation updated = Validation.createNew("CODE", "DESC", ValidationDataType.TEXT, "creator")
                .updateBasics("NEW DESC", "upd");
        when(updateUC.execute(eq("CODE"), anyString(), any())).thenReturn(Mono.just(updated));

        ValidationUpdateRequest request = new ValidationUpdateRequest("CODE", "NEW DESC", "TEXT", null, null, null, "upd");

        client.put().uri("/validations/update/CODE")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.description").isEqualTo("NEW DESC");
    }

    @Test
    void changeValidationStatusEndpointValidatesBody() {
        Validation changed = Validation.createNew("CODE", "DESC", ValidationDataType.TEXT, "creator")
                .changeStatus("I", "upd");
        when(changeStatusUC.execute(eq("CODE"), eq("I"), any())).thenReturn(Mono.just(changed));

        ValidationStatusRequest request = new ValidationStatusRequest("I", "upd");

        client.put().uri("/validations/update/status/CODE")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("I");
    }

    @Test
    void attachRuleEndpointPassesPayload() {
        ValidationMap mapped = ValidationMap.createNew("ST", "123456", 5L, "SI", null, null, "upd");
        when(mapRuleUC.attach(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Mono.just(mapped));

        MapRuleRequest request = new MapRuleRequest("ST", "123456", "CODE", true, "upd");

        client.post().uri("/validations/attach")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.validationId").isEqualTo(5);
    }

    @Test
    void changeRuleStatusEndpointUsesPathVariables() {
        ValidationMap mapped = ValidationMap.createNew("ST", "123456", 5L, null, null, null, "upd")
                .changeStatus("I", "upd");
        when(mapRuleUC.changeStatus(eq("ST"), eq("123456"), eq("CODE"), eq("I"), any()))
                .thenReturn(Mono.just(mapped));

        ValidationStatusRequest request = new ValidationStatusRequest("I", "upd");

        client.put().uri("/validations/subtypes/ST/bins/123456/rules/status/CODE")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("I");
    }

    @Test
    void listRulesForSubtypeEndpointReturnsCollection() {
        ValidationMap mapped = ValidationMap.createNew("ST", "123456", 5L, null, null, null, "upd");
        when(listRulesUC.execute(any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(Flux.just(mapped));
        when(listRulesUC.execute(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Flux.just(mapped));

        client.get().uri("/v1/subtypes/ST/rules?status=A&page=0&size=10")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].validationId").isEqualTo(5);
    }
}

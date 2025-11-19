package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.model.PlanItemsBulkResult;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security.ActorProvider;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.router.PlanRouter;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlanHandlerTest {

    private CreatePlanUseCase createUC;
    private GetPlanUseCase getUC;
    private ListPlansUseCase listUC;
    private UpdatePlanUseCase updateUC;
    private ChangePlanStatusUseCase changeStatusUC;
    private AddPlanItemUseCase addItemUC;
    private ListPlanItemsUseCase listItemsUC;
    private AssignPlanToSubtypeUseCase assignUC;
    private ChangePlanItemStatusUseCase changeItemStatusUC;
    private ValidationUtil validation;
    private ActorProvider actorProvider;
    private WebTestClient client;

    @BeforeEach
    void setup() {
        createUC = mock(CreatePlanUseCase.class);
        getUC = mock(GetPlanUseCase.class);
        listUC = mock(ListPlansUseCase.class);
        updateUC = mock(UpdatePlanUseCase.class);
        changeStatusUC = mock(ChangePlanStatusUseCase.class);
        addItemUC = mock(AddPlanItemUseCase.class);
        listItemsUC = mock(ListPlanItemsUseCase.class);
        assignUC = mock(AssignPlanToSubtypeUseCase.class);
        changeItemStatusUC = mock(ChangePlanItemStatusUseCase.class);
        validation = mock(ValidationUtil.class);
        actorProvider = mock(ActorProvider.class);

        PlanHandler handler = new PlanHandler(createUC, getUC, listUC, updateUC, changeStatusUC,
                addItemUC, listItemsUC, assignUC, validation, changeItemStatusUC, actorProvider);
        PlanRouter router = new PlanRouter(handler);
        try {
            var method = PlanRouter.class.getDeclaredMethod("routes");
            method.setAccessible(true);
            client = WebTestClient.bindToRouterFunction((RouterFunction<ServerResponse>) method.invoke(router))
                    .configureClient().baseUrl("/").build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(validation.validate(any(), any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(actorProvider.currentUserId()).thenReturn(Mono.just("secUser"));
    }

    @Test
    void createEndpointReturnsCreatedEnvelope() {
        CommercePlan created = CommercePlan.rehydrate(5L, "PLAN1", "Name", CommerceValidationMode.MERCHANT_ID,
                "desc", "A", OffsetDateTime.now(), OffsetDateTime.now(), "creator");
        when(createUC.execute(any(), any(), any(), any(), any())).thenReturn(Mono.just(created));

        PlanCreateRequest request = new PlanCreateRequest("plan1", "Name", CommerceValidationMode.MERCHANT_ID, "desc", "creator");

        client.post().uri("/plans/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.code").isEqualTo("PLAN1");
    }

    @Test
    void changeStatusEndpointUsesPathVariable() {
        CommercePlan plan = CommercePlan.rehydrate(10L, "PLAN1", "Name", CommerceValidationMode.MERCHANT_ID,
                "desc", "I", OffsetDateTime.now(), OffsetDateTime.now(), "actor");
        when(changeStatusUC.execute(eq("PLAN1"), eq("I"), any())).thenReturn(Mono.just(plan));

        PlanStatusRequest body = new PlanStatusRequest("I", "actor");
        client.put().uri("/plans/update/status/PLAN1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("I");
    }

    @Test
    void listItemsValidatesRequiredPlanCode() {
        client.get().uri("/plans/items/get/%20")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void addItemsSupportsBulkPayload() {
        PlanItemsBulkResult bulkResult = new PlanItemsBulkResult("PLAN", 2, 1, 1, 0, List.of(), List.of("1000"));
        when(addItemUC.addMany(eq("PLAN"), any(), any())).thenReturn(Mono.just(bulkResult));

        PlanItemRequest request = new PlanItemRequest("PLAN", null, List.of("1000", "1001"), "user");

        client.post().uri("/plans/items/attach")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.inserted").isEqualTo(1);
    }

    @Test
    void assignToSubtypeReturnsLink() {
        SubtypePlanLink link = SubtypePlanLink.rehydrate("SUB", 99L, OffsetDateTime.now(), OffsetDateTime.now(), "actor");
        when(assignUC.assign(eq("SUB"), eq("PLAN"), any())).thenReturn(Mono.just(link));

        AssignPlanRequest request = new AssignPlanRequest("SUB", "PLAN", "actor");

        client.post().uri("/plans/assign/subtype")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.planId").isEqualTo(99);
    }

    @Test
    void listPlansPassesThroughQueryParams() {
        CommercePlan plan = CommercePlan.rehydrate(1L, "PLAN", "Name", CommerceValidationMode.MERCHANT_ID,
                "desc", "A", OffsetDateTime.now(), OffsetDateTime.now(), "actor");
        when(listUC.execute("A", "PLAN", 0, 10)).thenReturn(Flux.just(plan));

        client.get().uri("/plans/list?status=A&q=PLAN&page=0&size=10")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].code").isEqualTo("PLAN");
    }
}

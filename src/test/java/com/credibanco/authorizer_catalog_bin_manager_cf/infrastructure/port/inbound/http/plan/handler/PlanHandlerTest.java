package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.model.PlanItemsBulkResult;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
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
        ValidationUtil validation = mock(ValidationUtil.class);
        ActorProvider actorProvider = mock(ActorProvider.class);

        PlanHandler handler = new PlanHandler(
                createUC, getUC, listUC, updateUC, changeStatusUC,
                addItemUC, listItemsUC, assignUC, validation, changeItemStatusUC, actorProvider
        );
        PlanRouter router = new PlanRouter(handler);

        try {
            var method = PlanRouter.class.getDeclaredMethod("routes");
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            RouterFunction<ServerResponse> routes =
                    (RouterFunction<ServerResponse>) method.invoke(router);

            client = WebTestClient.bindToRouterFunction(routes)
                    .configureClient()
                    .baseUrl("/")
                    .build();
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
    void getEndpointReturnsEnvelope() {
        CommercePlan created = CommercePlan.rehydrate(5L, "PLAN1", "Name", CommerceValidationMode.MERCHANT_ID,
                "desc", "A", OffsetDateTime.now(), OffsetDateTime.now(), "creator");
        when(getUC.execute("PLAN1")).thenReturn(Mono.just(created));

        client.get().uri("/plans/get/PLAN1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
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
    void changeStatusResolvesActorFromHeaderWhenBodyBlank() {
        CommercePlan plan = CommercePlan.rehydrate(10L, "PLAN1", "Name", CommerceValidationMode.MERCHANT_ID,
                "desc", "I", OffsetDateTime.now(), OffsetDateTime.now(), "header-actor");
        when(changeStatusUC.execute(eq("PLAN1"), eq("I"), eq("header-actor"))).thenReturn(Mono.just(plan));

        PlanStatusRequest body = new PlanStatusRequest("I", " ");
        client.put().uri("/plans/update/status/PLAN1")
                .header("X-User", "header-actor")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.updatedBy").isEqualTo("header-actor");
    }

    @Test
    void listItemsValidatesRequiredPlanCode() {
        when(getUC.execute("%20")).thenReturn(Mono.error(new RuntimeException("invalid")));
        client.get().uri("/plans/items/get/%20")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void listItemsRejectsNonNumericPaginationValues() {
        client.get().uri("/plans/items/get/PLAN?page=abc")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void listItemsReturnsDetailWhenEmptyUsingAllStatus() {
        CommercePlan plan = CommercePlan.rehydrate(1L, "PLAN", "Name", CommerceValidationMode.MERCHANT_ID,
                "desc", "A", OffsetDateTime.now(), OffsetDateTime.now(), "actor");
        when(getUC.execute("PLAN")).thenReturn(Mono.just(plan));
        when(listItemsUC.list("PLAN", 0, 100, null)).thenReturn(Flux.empty());

        client.get().uri("/plans/items/get/PLAN?status=ALL")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.detail").doesNotExist()
                .jsonPath("$.detail").isEqualTo("El plan no tiene ítems");
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
    void addItemSingleValueCreatesResource() {
        PlanItem item = PlanItem.rehydrate(55L, 5L, "1000", OffsetDateTime.now(), OffsetDateTime.now(), "user", "A");
        when(addItemUC.addValue("PLAN", "1000", "headerUser")).thenReturn(Mono.just(item));

        PlanItemRequest request = new PlanItemRequest("PLAN", "1000", null, null);

        client.post().uri("/plans/items/attach")
                .header("X-User", "headerUser")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.planItemId").isEqualTo(55);
    }

    @Test
    void addItemRequiresValueOrValues() {
        PlanItemRequest request = new PlanItemRequest("PLAN", " ", List.of(), null);

        client.post().uri("/plans/items/attach")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
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
    void changeItemStatusReturnsEnvelope() {
        PlanItem item = PlanItem.rehydrate(1L, 1L, "1000", OffsetDateTime.now(), OffsetDateTime.now(), "actor", "I");
        when(changeItemStatusUC.execute("PLAN", "1000", "I", "secUser")).thenReturn(Mono.just(item));

        PlanItemStatus request = new PlanItemStatus("PLAN", "1000", "I", null);
        client.put().uri("/plans/items/update/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("I");
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

    @Test
    void listPlansReturnsEmptyDetailWhenNoResults() {
        when(listUC.execute(null, null, 0, 20)).thenReturn(Flux.empty());

        client.get().uri("/plans/list")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Sin planes para el filtro");
    }

    @Test
    void updatePlanUsesNormalizedValuesAndSecurityActorFallback() {
        CommercePlan plan = CommercePlan.rehydrate(1L, "PLAN1", "NOMBRE", CommerceValidationMode.MERCHANT_ID,
                "DESC", "A", OffsetDateTime.now(), OffsetDateTime.now(), "secUser");
        when(updateUC.execute("PLAN1", "NOMBRE", "DESC", "MERCHANT_ID", "secUser")).thenReturn(Mono.just(plan));

        PlanUpdateRequest request = new PlanUpdateRequest("nombre", "desc", CommerceValidationMode.MERCHANT_ID, null);

        client.put().uri("/plans/update/plan1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.code").isEqualTo("PLAN1");
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security.ActorProvider;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto.AgencyCreateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto.AgencyStatusRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto.AgencyUpdateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.router.AgencyRouter;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AgencyHandlerTest {

    private CreateAgencyUseCase createUC;
    private UpdateAgencyUseCase updateUC;
    private ChangeAgencyStatusUseCase changeStatusUC;
    private GetAgencyUseCase getUC;
    private ListAgenciesUseCase listUC;
    private WebTestClient client;

    @BeforeEach
    void setup() {
        createUC = mock(CreateAgencyUseCase.class);
        updateUC = mock(UpdateAgencyUseCase.class);
        changeStatusUC = mock(ChangeAgencyStatusUseCase.class);
        getUC = mock(GetAgencyUseCase.class);
        listUC = mock(ListAgenciesUseCase.class);
        ValidationUtil validation = mock(ValidationUtil.class);
        ActorProvider actorProvider = mock(ActorProvider.class);

        AgencyHandler handler = new AgencyHandler(createUC, updateUC, changeStatusUC, getUC, listUC, validation, actorProvider);
        AgencyRouter router = new AgencyRouter(handler);
        try {
            var method = AgencyRouter.class.getDeclaredMethod("routes");
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
    void createUsesBodyActorAndNormalizesFields() {
        when(createUC.execute(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        AgencyCreateRequest request = new AgencyCreateRequest(
                "abc", "01", "name", "123", "street", "555",
                "11001", "highlight", "7",
                "primary", "pid", "secondary", "sid",
                "pinPrimary", "ppid", "pinSec", "psid",
                "desc", "bodyUser"
        );

        client.post().uri("/agencies/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.subtypeCode").isEqualTo("ABC")
                .jsonPath("$.data.createdAt").isNotEmpty()
                .jsonPath("$.data.updatedBy").isEqualTo("bodyUser");
    }

    @Test
    void updateFallsBackToSecurityContextWhenNoActorProvided() {
        ArgumentCaptor<Agency> captor = ArgumentCaptor.forClass(Agency.class);
        when(updateUC.execute(captor.capture())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        AgencyUpdateRequest request = new AgencyUpdateRequest(
                "name", "123", "street", "555", "11001", "h", "7",
                "primary", "pid", "secondary", "sid",
                "pinPrimary", "ppid", "pinSec", "psid",
                "desc", " "
        );

        client.put().uri("/agencies/update/abc/01")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.updatedBy").isEqualTo("secUser");

        Agency updated = captor.getValue();
        assertThat(updated.updatedBy()).isEqualTo("secUser");
    }

    @Test
    void changeStatusUsesHeaderActorWhenPresent() {
        when(changeStatusUC.execute(anyString(), anyString(), anyString(), any())).thenAnswer(inv ->
                Mono.just(Agency.rehydrate("ST", "01", "NAME", "NIT", "ADDR", "PHONE", "11001",
                        "H", "7", "P", "PID", "S", "SID", "PP", "PPID", "PS", "PSID",
                        "DESC", inv.getArgument(2), OffsetDateTime.MIN, OffsetDateTime.MIN, inv.getArgument(3))));

        AgencyStatusRequest request = new AgencyStatusRequest("I", null);

        client.put().uri("/agencies/update/status/ST/01")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User", " headerUser ")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("I")
                .jsonPath("$.data.updatedBy").isEqualTo(" headerUser ");
    }

    @Test
    void getReturnsEnvelope() {
        when(getUC.execute("ST", "01")).thenReturn(Mono.just(
                Agency.rehydrate("ST", "01", "NAME", "NIT", "ADDR", "PHONE", "11001",
                        "H", "7", "P", "PID", "S", "SID", "PP", "PPID", "PS", "PSID",
                        "DESC", "A", OffsetDateTime.MIN, OffsetDateTime.MIN, "actor")));

        client.get().uri("/agencies/get/ST/01")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.agencyCode").isEqualTo("01");
    }

    @Test
    void listValidatesStatusValues() {
        when(listUC.execute(anyString(), anyString(), any(), anyInt(), anyInt()))
                .thenReturn(Flux.empty());

        client.get().uri("/agencies/list/ST?subtypeCode=ST&status=A&page=0&size=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").isArray();

        client.get().uri("/agencies/list/ST?status=INVALID")
                .exchange()
                .expectStatus().is5xxServerError();
    }
}

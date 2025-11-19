package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.handler;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.inbound.*;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.http.ApiError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.config.security.ActorProvider;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto.BinCreateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto.BinStatusUpdateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto.BinUpdateRequest;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.router.BinRouter;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BinHandlerTest {

    private CreateBinUseCase createUC;
    private ListBinsUseCase listUC;
    private UpdateBinUseCase updateUC;
    private GetBinUseCase getUC;
    private ChangeBinStatusUseCase changeStatusUC;
    private ValidationUtil validation;
    private ActorProvider actorProvider;
    private WebTestClient client;

    @BeforeEach
    void setup() {
        createUC = mock(CreateBinUseCase.class);
        listUC = mock(ListBinsUseCase.class);
        updateUC = mock(UpdateBinUseCase.class);
        getUC = mock(GetBinUseCase.class);
        changeStatusUC = mock(ChangeBinStatusUseCase.class);
        validation = mock(ValidationUtil.class);
        actorProvider = mock(ActorProvider.class);

        BinHandler handler = new BinHandler(createUC, listUC, validation, updateUC, getUC, changeStatusUC, actorProvider);
        BinRouter router = new BinRouter(handler);
        client = WebTestClient.bindToRouterFunction(router.routes()).configureClient().baseUrl("/").build();

        when(validation.validate(any(), any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(actorProvider.currentUserId()).thenReturn(Mono.just("secUser"));
    }

    @Test
    void createEndpointReturnsCreatedEnvelope() {
        Bin created = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, "creator");
        when(createUC.execute(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(created));

        BinCreateRequest request = new BinCreateRequest("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, "creator");

        client.post().uri("/bins/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.bin").isEqualTo("123456");
    }

    @Test
    void getEndpointRejectsInvalidPath() {
        client.get().uri("/bins/get/12ab")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void listEndpointUsesPagination() {
        Bin bin = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null);
        when(listUC.execute(0, 20)).thenReturn(Flux.just(bin));

        client.get().uri("/bins/list?page=0&size=20")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data[0].bin").isEqualTo("123456");
    }

    @Test
    void updateEndpointPassesPayload() {
        Bin updated = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null);
        when(updateUC.execute(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(updated));

        BinUpdateRequest request = new BinUpdateRequest("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, "actor");

        client.put().uri("/bins/update")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.bin").isEqualTo("123456");
    }

    @Test
    void changeStatusEndpointValidatesBody() {
        Bin aggregate = Bin.createNew("123456", "NAME", "DEBITO", "12", "CC", "DESC", "N", null, null);
        when(changeStatusUC.execute(eq("123456"), eq("I"), any())).thenReturn(Mono.just(aggregate.changeStatus("I", "u")));

        BinStatusUpdateRequest request = new BinStatusUpdateRequest("I", "actor");

        client.put().uri("/bins/update/status/123456")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("I");
    }
}

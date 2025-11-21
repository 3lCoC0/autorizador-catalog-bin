package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GlobalErrorHandlerTest {

    private GlobalErrorHandler handler;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new GlobalErrorHandler();
    }

    @Test
    void returnsErrorWhenResponseAlreadyCommitted() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/bins/1").build());
        exchange.getResponse().setComplete().block();
        RuntimeException problem = new RuntimeException("committed");

        StepVerifier.create(handler.handle(exchange, problem))
                .expectErrorSatisfies(t -> assertSame(problem, t))
                .verify();
    }

    @Test
    void handlesAppExceptionWithCorrelationId() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/bins/10").build());
        AppException ex = new AppException(AppError.BIN_NOT_FOUND, "custom msg");

        handler.handle(exchange, ex).block();

        MockServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        String cid = response.getHeaders().getFirst("X-Correlation-Id");
        assertThat(cid).isNotBlank();

        String body = response.getBodyAsString().block();
        var json = mapper.readTree(body);
        assertThat(json.get("responseCode").asText()).isEqualTo(AppError.BIN_NOT_FOUND.code);
        assertThat(json.get("message").asText()).isEqualTo("custom msg");
        assertThat(json.get("path").asText()).isEqualTo("/bins/10");
    }

    @Test
    void mapsValidationErrorsUsingJacksonMessages() throws Exception {
        var invalid = Mockito.mock(com.fasterxml.jackson.databind.exc.InvalidFormatException.class);
        Mockito.when(invalid.getOriginalMessage()).thenReturn("Invalid number");
        DecodingException decodingException = new DecodingException("decode", invalid);

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/plans/9").build());
        handler.handle(exchange, decodingException).block();

        MockServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String body = response.getBodyAsString().block();
        var json = mapper.readTree(body);
        assertThat(json.get("responseCode").asText()).isEqualTo(AppError.PLAN_INVALID_DATA.code);
        assertThat(json.get("message").asText()).contains("Invalid number");
    }

    @Test
    void honorsResponseStatusExceptionReason() throws Exception {
        ResponseStatusException rse = new ResponseStatusException(HttpStatus.NOT_FOUND, "gone");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/unknown").build());

        handler.handle(exchange, rse).block();

        MockServerHttpResponse response = exchange.getResponse();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        var json = mapper.readTree(response.getBodyAsString().block());
        assertThat(json.get("message").asText()).isEqualTo("gone");
        assertThat(json.get("responseCode").asText()).isEqualTo(AppError.INTERNAL.code);
    }

    @Test
    void mapsNotFoundAndConflictShortcuts() throws Exception {
        MockServerWebExchange notFoundExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/bins/404").build());
        handler.handle(notFoundExchange, new GlobalErrorHandler.NotFoundException("missing")).block();
        MockServerHttpResponse notFoundResponse = notFoundExchange.getResponse();
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatusCode());
        assertThat(mapper.readTree(notFoundResponse.getBodyAsString().block()).get("message").asText())
                .isEqualTo(AppError.BIN_NOT_FOUND.defaultMessage);

        MockServerWebExchange conflictExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/bins/1").build());
        handler.handle(conflictExchange, new IllegalStateException("conflict!")).block();
        MockServerHttpResponse conflictResponse = conflictExchange.getResponse();
        assertEquals(HttpStatus.CONFLICT, conflictResponse.getStatusCode());
        assertThat(mapper.readTree(conflictResponse.getBodyAsString().block()).get("message").asText())
                .isEqualTo(AppError.BIN_ALREADY_EXISTS.defaultMessage);
    }

    @Test
    void mapsTimeoutsAndUnknownErrors() throws Exception {
        MockServerWebExchange timeoutExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/timeout").build());
        handler.handle(timeoutExchange, new RuntimeException(new TimeoutException("late"))).block();
        MockServerHttpResponse timeoutResponse = timeoutExchange.getResponse();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, timeoutResponse.getStatusCode());
        assertThat(mapper.readTree(timeoutResponse.getBodyAsString().block()).get("message").asText())
                .contains("tiempo de espera");

        MockServerWebExchange unknownExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/unknown").build());
        handler.handle(unknownExchange, new RuntimeException("boom")).block();
        MockServerHttpResponse unknownResponse = unknownExchange.getResponse();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, unknownResponse.getStatusCode());
        assertThat(mapper.readTree(unknownResponse.getBodyAsString().block()).get("message").asText())
                .isEqualTo("Se produjo un error inesperado.");
    }

    @Test
    void exercisesUtilityMethodsViaReflection() throws Exception {
        Method isBinPath = GlobalErrorHandler.class.getDeclaredMethod("isBinPath", String.class);
        isBinPath.setAccessible(true);
        assertThat(isBinPath.invoke(handler, "/bins/1")).isEqualTo(true);
        assertThat(isBinPath.invoke(handler, "/other")).isEqualTo(false);

        Method isPlanPath = GlobalErrorHandler.class.getDeclaredMethod("isPlanPath", String.class);
        isPlanPath.setAccessible(true);
        assertThat(isPlanPath.invoke(handler, "/plans/1")).isEqualTo(true);

        Method validation = GlobalErrorHandler.class.getDeclaredMethod("mapFallbackValidation", String.class);
        validation.setAccessible(true);
        Object mappedValidation = validation.invoke(handler, "/bins/1");
        assertMapped(mappedValidation, AppError.BIN_INVALID_DATA, AppError.BIN_INVALID_DATA.defaultMessage);

        Method notFound = GlobalErrorHandler.class.getDeclaredMethod("mapFallbackNotFound", String.class);
        notFound.setAccessible(true);
        Object mappedNotFound = notFound.invoke(handler, "/else");
        assertMapped(mappedNotFound, AppError.INTERNAL, "Recurso no encontrado");

        Method conflict = GlobalErrorHandler.class.getDeclaredMethod("mapFallbackConflict", String.class);
        conflict.setAccessible(true);
        Object mappedConflict = conflict.invoke(handler, "/bins/1");
        assertMapped(mappedConflict, AppError.BIN_ALREADY_EXISTS, AppError.BIN_ALREADY_EXISTS.defaultMessage);

        Method mapByStatus = GlobalErrorHandler.class.getDeclaredMethod("mapFallbackByStatus", String.class, HttpStatus.class);
        mapByStatus.setAccessible(true);
        assertMapped(mapByStatus.invoke(handler, "/x", HttpStatus.BAD_REQUEST), AppError.INTERNAL, "Solicitud inválida");
        assertMapped(mapByStatus.invoke(handler, "/x", HttpStatus.CONFLICT), AppError.INTERNAL, "Conflicto de negocio");
        assertMapped(mapByStatus.invoke(handler, "/x", HttpStatus.I_AM_A_TEAPOT), AppError.INTERNAL, "Se produjo un error inesperado");

        Method trimOrNull = GlobalErrorHandler.class.getDeclaredMethod("trimOrNull", String.class);
        trimOrNull.setAccessible(true);
        assertThat(trimOrNull.invoke(null, " spaced ")).isEqualTo("spaced");

        Method safeMessage = GlobalErrorHandler.class.getDeclaredMethod("safeMessage", String.class, String.class);
        safeMessage.setAccessible(true);
        assertThat(safeMessage.invoke(null, "hello", "fallback")).isEqualTo("hello");
        assertThat(safeMessage.invoke(null, "  ", "fallback")).isEqualTo("fallback");

        Method truncate = GlobalErrorHandler.class.getDeclaredMethod("truncate", String.class);
        truncate.setAccessible(true);

        assertThat(truncate.invoke(null, "abc")).isEqualTo("abc");

        String longStr = "abcdef";
        assertThat(truncate.invoke(null, longStr)).isEqualTo("abcdef");

        String longStr2 = "x".repeat(600);
        String result = (String) truncate.invoke(null, longStr2);
        assertThat(result).hasSize(503);
        assertThat(result).endsWith("...");

        Method mostSpecific = GlobalErrorHandler.class.getDeclaredMethod("mostSpecific", Throwable.class);
        mostSpecific.setAccessible(true);
        Throwable deep = new RuntimeException(new IllegalStateException(new IllegalArgumentException("bottom")));
        assertThat(((Throwable) mostSpecific.invoke(null, deep)).getMessage()).isEqualTo("bottom");

        Method illegalArgumentMessage = GlobalErrorHandler.class.getDeclaredMethod("illegalArgumentMessage", Throwable.class);
        illegalArgumentMessage.setAccessible(true);
        assertThat(illegalArgumentMessage.invoke(null, deep)).isEqualTo("bottom");

        Method jacksonMessageOrNull = GlobalErrorHandler.class.getDeclaredMethod("jacksonMessageOrNull", Throwable.class);
        jacksonMessageOrNull.setAccessible(true);
        var mismatched = Mockito.mock(com.fasterxml.jackson.databind.exc.MismatchedInputException.class);
        Mockito.when(mismatched.getOriginalMessage()).thenReturn("mismatch");
        assertThat(jacksonMessageOrNull.invoke(null, mismatched)).isEqualTo("mismatch");

        var invalidFormat = Mockito.mock(com.fasterxml.jackson.databind.exc.InvalidFormatException.class);
        Mockito.when(invalidFormat.getOriginalMessage()).thenReturn("invalid");
        assertThat(jacksonMessageOrNull.invoke(null, invalidFormat)).isEqualTo("invalid");

        var valueInstantiation = Mockito.mock(com.fasterxml.jackson.databind.exc.ValueInstantiationException.class);
        Mockito.when(valueInstantiation.getCause()).thenReturn(new IllegalArgumentException("iae"));
        assertThat(jacksonMessageOrNull.invoke(null, valueInstantiation)).isEqualTo("iae");

        Method unwrapMessage = GlobalErrorHandler.class.getDeclaredMethod("unwrapMessage", Throwable.class);
        unwrapMessage.setAccessible(true);
        ResponseStatusException wrapped = new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad body", new IllegalArgumentException("deep"));
        ServerWebInputException swe = new ServerWebInputException("error", null, wrapped);
        assertThat(unwrapMessage.invoke(handler, swe)).isEqualTo("deep");

        Method findCause = GlobalErrorHandler.class.getDeclaredMethod("findCause", Throwable.class, Class.class);
        findCause.setAccessible(true);
        RuntimeException chained = new RuntimeException(new SocketTimeoutException("too slow"));
        assertThat(findCause.invoke(null, chained, SocketTimeoutException.class)).isInstanceOf(SocketTimeoutException.class);

        Method isTimeout = GlobalErrorHandler.class.getDeclaredMethod("isTimeout", Throwable.class);
        isTimeout.setAccessible(true);
        assertThat(isTimeout.invoke(handler, chained)).isEqualTo(true);
    }

    private void assertMapped(Object mapped, AppError expectedError, String expectedMessage) throws Exception {
        Method errorMethod = mapped.getClass().getDeclaredMethod("error");
        errorMethod.setAccessible(true);
        Method messageMethod = mapped.getClass().getDeclaredMethod("message");
        messageMethod.setAccessible(true);
        assertThat(errorMethod.invoke(mapped)).isEqualTo(expectedError);
        assertThat(messageMethod.invoke(mapped)).isEqualTo(expectedMessage);
    }
}

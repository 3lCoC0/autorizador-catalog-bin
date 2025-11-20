package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NullOrBlankToNullIntegerDeserializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Integer.class, new NullOrBlankToNullIntegerDeserializer());
        mapper.registerModule(module);
    }

    @Test
    void deserializesNullAndBlankStringsToNull() throws Exception {
        assertThat(mapper.readValue("null", Integer.class)).isNull();
        assertThat(mapper.readValue("\"   \"", Integer.class)).isNull();
        assertThat(mapper.readValue("\"null\"", Integer.class)).isNull();
    }

    @Test
    void acceptsValidNumericStringsAndNumbers() throws Exception {
        assertThat(mapper.readValue("\"1\"", Integer.class)).isEqualTo(1);
        assertThat(mapper.readValue("2", Integer.class)).isEqualTo(2);
        assertThat(mapper.readValue("\"3\"", Integer.class)).isEqualTo(3);
    }

    @Test
    void rejectsOutOfRangeNumbers() {
        assertThatThrownBy(() -> mapper.readValue("\"0\"", Integer.class))
                .isInstanceOf(MismatchedInputException.class);
        assertThatThrownBy(() -> mapper.readValue("\"5\"", Integer.class))
                .isInstanceOf(MismatchedInputException.class);
        assertThatThrownBy(() -> mapper.readValue("4", Integer.class))
                .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void rejectsNonNumericValues() {
        assertThatThrownBy(() -> mapper.readValue("\"abc\"", Integer.class))
                .isInstanceOf(MismatchedInputException.class);
        assertThatThrownBy(() -> mapper.readValue("true", Integer.class))
                .isInstanceOf(MismatchedInputException.class);
        assertThatThrownBy(() -> mapper.readValue("{}", Integer.class))
                .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void returnsNullWhenParserTextIsNull() throws Exception {
        JsonParser parser = Mockito.mock(JsonParser.class);
        DeserializationContext context = Mockito.mock(DeserializationContext.class);

        when(parser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
        when(parser.getText()).thenReturn(null);

        Integer value = new NullOrBlankToNullIntegerDeserializer().deserialize(parser, context);

        assertThat(value).isNull();
        verify(context, never()).reportInputMismatch(eq(Integer.class), anyString());
    }
}

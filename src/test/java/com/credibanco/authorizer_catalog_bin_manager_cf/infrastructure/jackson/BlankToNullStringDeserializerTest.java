package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlankToNullStringDeserializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new BlankToNullStringDeserializer());
        mapper.registerModule(module);
    }

    @Test
    void trimsAndConvertsBlankStringsToNull() throws Exception {
        assertThat(mapper.readValue("\"  text  \"", String.class)).isEqualTo("text");
        assertThat(mapper.readValue("\"   \"", String.class)).isNull();
        assertThat(mapper.readValue("null", String.class)).isNull();
    }

    @Test
    void handlesNonStringTokens() throws Exception {
        assertThat(mapper.readValue("true", String.class)).isEqualTo("true");
        assertThat(mapper.readValue("123", String.class)).isEqualTo("123");
        assertThat(mapper.readValue("{}", String.class)).isNull();
    }
}

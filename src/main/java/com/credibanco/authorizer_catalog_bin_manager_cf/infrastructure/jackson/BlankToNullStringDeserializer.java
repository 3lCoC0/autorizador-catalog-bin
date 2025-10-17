package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Convierte cadenas vacías o con espacios, y el literal "null" (ignora mayúsculas/minúsculas),
 * en null. Además aplica trim() a los strings no nulos.
 */
public class BlankToNullStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();

        if (token == JsonToken.VALUE_STRING) {
            String raw = p.getText();
            if (raw == null) return null;

            String t = raw.trim();
            if (t.isEmpty() || "null".equalsIgnoreCase(t)) {
                return null;
            }
            return t; // devuelve la cadena ya normalizada (trim)
        }

        if (token == JsonToken.VALUE_NULL) {
            return null;
        }

        // Para otros tipos que se puedan mapear a String
        String val = p.getValueAsString();
        if (val == null) return null;

        String t = val.trim();
        return (t.isEmpty() || "null".equalsIgnoreCase(t)) ? null : t;
    }
}
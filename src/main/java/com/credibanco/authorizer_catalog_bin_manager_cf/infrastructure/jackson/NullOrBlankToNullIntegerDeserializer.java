package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class NullOrBlankToNullIntegerDeserializer extends JsonDeserializer<Integer> {

    private static final String MSG =
            "binExtDigits solo permite los numeros 1, 2 o 3 (o la palabra null cuando usesBinExt es igual a N).";

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();

        // null JSON → null
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }

        // Cadenas
        if (token == JsonToken.VALUE_STRING) {
            String raw = p.getText();
            if (raw == null) return null;

            String t = raw.trim();
            // "" o "null" (case-insensitive) → null
            if (t.isEmpty() || "null".equalsIgnoreCase(t)) {
                return null;
            }

            // Solo aceptar dígitos
            if (!t.matches("^\\d+$")) {
                // Mensaje controlado (evita "Failed to read HTTP message")
                ctxt.reportInputMismatch(Integer.class, MSG);
                return null; // unreachable, pero requerido por el compilador
            }

            int v = Integer.parseInt(t);
            if (v < 1 || v > 3) {
                ctxt.reportInputMismatch(Integer.class, MSG);
            }
            return v;
        }

        // Números (JSON number)
        if (token == JsonToken.VALUE_NUMBER_INT) {
            int v = p.getIntValue();
            if (v < 1 || v > 3) {
                ctxt.reportInputMismatch(Integer.class, MSG);
            }
            return v;
        }

        // Cualquier otro tipo (boolean, objeto, array, number float, etc.)
        ctxt.reportInputMismatch(Integer.class, MSG);
        return null; // unreachable
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ValidationDataType {
    BOOL, NUMBER, TEXT;

    public static final String ALLOWED = "BOOL | NUMBER | TEXT";

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ValidationDataType fromJson(Object raw) {
        if (raw == null) {
            throw new IllegalArgumentException("dataType requerido (" + ALLOWED + ")");
        }
        String s = String.valueOf(raw).trim().toUpperCase();

        return switch (s) {
            case "BOOL"   -> BOOL;
            case "NUMBER" -> NUMBER;
            case "TEXT"   -> TEXT;
            default -> throw new IllegalArgumentException(
                    "dataType inválido: '" + s + "'. Valores permitidos: " + ALLOWED
            );
        };
    }
}

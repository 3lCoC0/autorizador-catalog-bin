package com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CommerceValidationMode {
    UNIQUE, MCC;

    public static final String ALLOWED = "UNIQUE | MCC";

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CommerceValidationMode fromJson(Object raw) {
        if (raw == null) {
            throw new IllegalArgumentException("validationMode requerido (" + ALLOWED + ")");
        }
        String s = String.valueOf(raw).trim().toUpperCase();
        return switch (s) {
            case "UNIQUE" -> UNIQUE;
            case "MCC"    -> MCC;
            default -> throw new IllegalArgumentException(
                    "validationMode inválido: '" + s + "'. Valores permitidos: " + ALLOWED
            );
        };
    }
}

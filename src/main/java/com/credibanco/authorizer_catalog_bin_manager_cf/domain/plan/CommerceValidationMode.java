package com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CommerceValidationMode {
    MERCHANT_ID, MCC;

    public static final String ALLOWED = "MERCHANT_ID | MCC";

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CommerceValidationMode fromJson(Object raw) {
        if (raw == null) {
            throw new IllegalArgumentException("validationMode requerido (" + ALLOWED + ")");
        }
        String s = String.valueOf(raw).trim().toUpperCase();
        return switch (s) {
            case "MERCHANT_ID" -> MERCHANT_ID;
            case "MCC"    -> MCC;
            default -> throw new IllegalArgumentException(
                    "validationMode inválido: '" + s + "'. Valores permitidos: " + ALLOWED
            );
        };
    }
}

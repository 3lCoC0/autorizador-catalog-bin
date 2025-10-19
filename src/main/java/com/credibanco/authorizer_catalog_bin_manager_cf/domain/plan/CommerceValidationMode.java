package com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.NotNull;

public enum CommerceValidationMode {
    MERCHANT_ID, MCC;

    public static final String ALLOWED = "MERCHANT_ID o MCC";

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CommerceValidationMode fromJson(Object raw) {
        if (raw == null) {
            throw new IllegalArgumentException("validationMode no puede ser nulo. Valores permitidos: " + ALLOWED);
        }
            String original = String.valueOf(raw);
        String normalized = getString(original);
        return switch (normalized) {
                case "MERCHANT_ID" -> MERCHANT_ID;
                case "MCC" -> MCC;
                default -> throw new IllegalArgumentException(
                        "validationMode inválido: '" + original + "'. Valores permitidos: " + ALLOWED
                );
            };
        }

    @NotNull
    private static String getString(String original) {
        String trimmed = original.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("validationMode no puede ser vacio. Valores permitidos: " + ALLOWED);
        }

        // Solo aceptamos letras y underscore (MERCHANT_ID) para evitar caracteres especiales.
        if (!trimmed.matches("^[A-Za-z_]+$")) {
            throw new IllegalArgumentException(
                    "validationMode inválido: '" + original + "'. Valores permitidos: " + ALLOWED
            );
        }

        return trimmed.toUpperCase();
    }
}
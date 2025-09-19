// domain/rule/Validation.java
package com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

public record Validation(
        Long   validationId,    // PK
        String code,            // unique
        String description,
        ValidationDataType dataType,
        String valueFlag,       // SI/NO (para BOOL)
        Double valueNum,        // para NUMBER
        String valueText,       // para TEXT
        String status,          // 'A'|'I'
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public Validation {
        require(code, "code");
        Objects.requireNonNull(dataType, "dataType");
        require(status, "status");
        if (!status.equals("A") && !status.equals("I"))
            throw new IllegalArgumentException("status debe ser A|I");
        // invariantes de tipo
        switch (dataType) {
            case BOOL -> {
                if (!"SI".equalsIgnoreCase(valueFlag) && !"NO".equalsIgnoreCase(valueFlag))
                    throw new IllegalArgumentException("value_flag debe ser SI/NO para BOOL");
                valueNum  = null; valueText = null;
            }
            case NUMBER -> {
                if (valueNum == null) throw new IllegalArgumentException("value_num requerido para NUMBER");
                valueFlag = null; valueText = null;
            }
            case TEXT -> {
                if (valueText == null || valueText.isBlank())
                    throw new IllegalArgumentException("value_text requerido para TEXT");
                valueFlag = null; valueNum = null;
            }
        }
    }

    public static Validation createNew(String code, String description, ValidationDataType type,
                                       String valueFlag, Double valueNum, String valueText, String createdBy /* audit externo */) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        return new Validation(null, code, description, type, valueFlag, valueNum, valueText,
                "A", now, null, now, now);
    }

    public static Validation rehydrate(Long id, String code, String description, ValidationDataType type,
                                       String flag, Double num, String text, String status,
                                       OffsetDateTime vf, OffsetDateTime vt,
                                       OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new Validation(id, code, description, type, flag, num, text, status, vf, vt, createdAt, updatedAt);
    }

    public Validation changeStatus(String newStatus) {
        if (!"A".equals(newStatus) && !"I".equals(newStatus))
            throw new IllegalArgumentException("status debe ser A|I");
        return new Validation(validationId, code, description, dataType, valueFlag, valueNum, valueText,
                newStatus, validFrom, validTo, createdAt, OffsetDateTime.now(ZoneOffset.UTC));
    }

    public Validation updateBasics(String newDescription,
                                   String newFlag, Double newNum, String newText) {
        return new Validation(validationId, code, newDescription, dataType,
                newFlag, newNum, newText, status, validFrom, validTo, createdAt,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    private static void require(String v, String f) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " es requerido");
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule;
import java.time.OffsetDateTime;
import java.util.Objects;

public record Validation(
        Long validationId,
        String code,
        String description,
        ValidationDataType dataType,
        String status,                 // 'A'|'I'
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {
    public Validation {
        require(code, "code");
        Objects.requireNonNull(dataType, "dataType");
        require(status, "status");
        if (!status.equals("A") && !status.equals("I"))
            throw new IllegalArgumentException("status debe ser A|I");
    }

    public static Validation createNew(String code, String description, ValidationDataType type, String createdBy) {
        var now = OffsetDateTime.now();
        return new Validation(null, code, description, type,
                "A", now, null, now, now, createdBy);
    }

    public static Validation rehydrate(Long id, String code, String description, ValidationDataType type,
                                       String status, OffsetDateTime vf, OffsetDateTime vt,
                                       OffsetDateTime createdAt, OffsetDateTime updatedAt, String updatedBy) {
        return new Validation(id, code, description, type, status, vf, vt, createdAt, updatedAt, updatedBy);
    }

    public Validation changeStatus(String newStatus, String by) {
        if (!"A".equals(newStatus) && !"I".equals(newStatus))
            throw new IllegalArgumentException("status debe ser A|I");
        return new Validation(validationId, code, description, dataType,
                newStatus, validFrom, validTo, createdAt, OffsetDateTime.now(), by);
    }

    public Validation updateBasics(String newDescription, String by) {
        return new Validation(validationId, code, newDescription, dataType,
                status, validFrom, validTo, createdAt, OffsetDateTime.now(), by);
    }

    private static void require(String v, String f) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " es requerido");
    }
}

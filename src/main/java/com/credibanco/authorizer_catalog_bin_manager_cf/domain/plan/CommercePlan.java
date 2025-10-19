package com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan;

import java.time.OffsetDateTime;
import java.util.Objects;

public record CommercePlan(
        Long planId,
        String code,
        String name,
        CommerceValidationMode validationMode, // UNIQUE | MCC
        String description,
        String status,            // 'A' | 'I'
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {

    public CommercePlan {
        require(code, "code");
        require(name, "name");
        Objects.requireNonNull(validationMode, "validationMode requerido");
        require(status, "status");
        if (!Objects.equals(status, "A") && !Objects.equals(status, "I")) {
            throw new IllegalArgumentException("status debe ser 'A' o 'I'");
        }
    }

    public static CommercePlan createNew(String code, String name, CommerceValidationMode mode,
                                         String descr, String by) {
        var now = OffsetDateTime.now();
        return new CommercePlan(null, code, name, mode, descr, "A", now, now, by);
    }

    // Rehidratación 1:1 desde la fila
    public static CommercePlan rehydrate(Long id, String code, String name, CommerceValidationMode mode,
                                         String descr, String status,
                                         OffsetDateTime createdAt, OffsetDateTime updatedAt, String updatedBy) {
        return new CommercePlan(id, code, name, mode, descr, status, createdAt, updatedAt, updatedBy);
    }

    // Igual a Agency.updateBasics(...): NO toca status ni createdAt
    public CommercePlan updateBasics(String newName, String newValidationMode /* puede venir null */,
                                     String newDescription, String by) {
        require(newName, "name");
        var mode = (newValidationMode == null)
                ? this.validationMode
                : CommerceValidationMode.fromJson(newValidationMode);
        return new CommercePlan(
                planId, code, newName, mode, newDescription,
                status, createdAt, OffsetDateTime.now(), by
        );
    }

    public CommercePlan changeStatus(String newStatus, String by) {
        if (!Objects.equals(newStatus, "A") && !Objects.equals(newStatus, "I")) {
            throw new IllegalArgumentException("status inválido (A|I)");
        }
        return new CommercePlan(
                planId, code, name, validationMode, description,
                newStatus, createdAt, OffsetDateTime.now(), by
        );
    }

    private static void require(String v, String f) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " es requerido");
    }
}

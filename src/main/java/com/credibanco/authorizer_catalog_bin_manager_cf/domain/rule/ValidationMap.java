// domain/rule/ValidationMap.java
package com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

public record ValidationMap(
        Long   mapId,
        String subtypeCode,
        String binEfectivo,   // 9 dígitos
        Long   validationId,
        Integer priority,     // 1..n
        String status,        // 'A'|'I'
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {
    public ValidationMap {
        require(subtypeCode, "subtypeCode");
        require(binEfectivo, "binEfectivo");
        Objects.requireNonNull(validationId, "validationId requerido");
        if (priority == null || priority < 1) throw new IllegalArgumentException("priority inválida");
        require(status, "status");
        if (!"A".equals(status) && !"I".equals(status))
            throw new IllegalArgumentException("status debe ser A|I");
    }

    public static ValidationMap createNew(String subtypeCode, String binEfectivo, Long validationId,
                                          Integer priority, String by) {
        var now = OffsetDateTime.now();
        return new ValidationMap(null, subtypeCode, binEfectivo, validationId, priority, "A",
                now, null, now, now, by);
    }

    public static ValidationMap rehydrate(Long id, String subtype, String eff, Long valId,
                                          Integer prio, String status,
                                          OffsetDateTime vf, OffsetDateTime vt,
                                          OffsetDateTime createdAt, OffsetDateTime updatedAt, String by) {
        return new ValidationMap(id, subtype, eff, valId, prio, status, vf, vt, createdAt, updatedAt, by);
    }

    public ValidationMap changeStatus(String newStatus, String by) {
        if (!"A".equals(newStatus) && !"I".equals(newStatus))
            throw new IllegalArgumentException("status debe ser A|I");
        return new ValidationMap(mapId, subtypeCode, binEfectivo, validationId, priority, newStatus,
                validFrom, validTo, createdAt, OffsetDateTime.now(), by);
    }

    private static void require(String v, String f) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " es requerido");
    }
}

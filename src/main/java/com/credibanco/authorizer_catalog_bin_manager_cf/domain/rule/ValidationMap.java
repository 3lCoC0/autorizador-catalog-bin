package com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule;
import java.time.OffsetDateTime;
import java.util.Objects;

public record ValidationMap(
        Long   mapId,
        String subtypeCode,
        String bin,
        Long   validationId,
        String status,                 // 'A'|'I'
        String valueFlag,              // SI|NO para BOOL
        Double valueNum,               // para NUMBER
        String valueText,              // para TEXT
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {
    public ValidationMap {
        require(subtypeCode, "subtypeCode");
        require(bin, "bin");
        Objects.requireNonNull(validationId, "validationId requerido");
        require(status, "status");
        if (!"A".equals(status) && !"I".equals(status))
            throw new IllegalArgumentException("status debe ser A|I");
    }



    public static ValidationMap createNew(String subtypeCode, String bin, Long validationId,
                                           String valueFlag, Double valueNum, String valueText, String by) {
        var now = OffsetDateTime.now();
        return new ValidationMap(null, subtypeCode, bin, validationId, "A",
                valueFlag, valueNum, valueText, now, null, by);
    }


    public static ValidationMap rehydrate( Long mapId,String subtypeCode,String bin,Long validationId,
                                           String status,String valueFlag,Double valueNum,String valueText,
                                           OffsetDateTime createdAt,
                                           OffsetDateTime updatedAt, String updatedBy)
    {

        return new ValidationMap(mapId,subtypeCode,bin,validationId,status,valueFlag,valueNum,valueText,
                    createdAt,updatedAt,updatedBy);
    }



    public ValidationMap changeStatus(String newStatus, String by) {
        if (!"A".equals(newStatus) && !"I".equals(newStatus))
            throw new IllegalArgumentException("status debe ser A|I");
        return new ValidationMap(mapId, subtypeCode, bin, validationId, newStatus,
                valueFlag, valueNum, valueText, createdAt, OffsetDateTime.now(), by);
    }

    private static void require(String v, String f) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " es requerido");
    }
}

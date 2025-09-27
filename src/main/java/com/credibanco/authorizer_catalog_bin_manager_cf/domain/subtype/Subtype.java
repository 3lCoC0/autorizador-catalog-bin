// domain/subtype/Subtype.java
package com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype;

import java.time.OffsetDateTime;
import java.util.Objects;

public record Subtype(
        String subtypeCode,
        String bin,
        String name,
        String description,
        String status,
        String ownerIdType,
        String ownerIdNumber,
        String binExt,        // puede ser null o N dígitos (según BIN maestro)
        String binEfectivo,   // = bin + (binExt o "")
        Long   subtypeId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy      // opcional (puede ser null)
) {
    public Subtype {
        require(subtypeCode, "subtypeCode");
        if (subtypeCode.length() != 3) throw new IllegalArgumentException("subtypeCode debe tener 3 caracteres");

        require(bin, "bin");
        if (!bin.chars().allMatch(Character::isDigit)) throw new IllegalArgumentException("bin debe ser numérico");
        final int len = bin.length();
        if (len < 6 || len > 9) throw new IllegalArgumentException("bin debe tener entre 6 y 9 dígitos");

        require(name, "name");

        require(status, "status");
        if (!Objects.equals(status, "A") && !Objects.equals(status, "I"))
            throw new IllegalArgumentException("status debe ser 'A' o 'I'");

        // Solo coherencia interna del agregado:
        // binEfectivo = concatenación literal de bin y binExt (si no es null)
        var expectedEff = computeBinEfectivo(bin, binExt);
        if (binEfectivo == null || !binEfectivo.equals(expectedEff))
            throw new IllegalArgumentException("binEfectivo inconsistente con bin/binExt");
    }

    public static Subtype createNew(String subtypeCode, String rawBin, String name,
                                    String description, String ownerIdType, String ownerIdNumber,
                                    String rawBinExt, String createdByNullable) {
        String normBin = digitsOnly(rawBin);
        String normExt = digitsOnlyOrNull(rawBinExt);
        String eff     = computeBinEfectivo(normBin, normExt);
        return new Subtype(
                subtypeCode, normBin, name, description, "I",
                ownerIdType, ownerIdNumber, normExt, eff, null, null, null, createdByNullable
        );
    }

    public Subtype updateBasics(String newName, String newDescription,
                                String newOwnerIdType, String newOwnerIdNumber,
                                String newRawBinExt, String byNullable) {
        require(newName, "name");
        String normExt = digitsOnlyOrNull(newRawBinExt);
        String eff     = computeBinEfectivo(this.bin, normExt);
        return new Subtype(
                this.subtypeCode, this.bin, newName, newDescription, this.status,
                newOwnerIdType, newOwnerIdNumber, normExt, eff, this.subtypeId,
                this.createdAt, OffsetDateTime.now(), byNullable
        );
    }

    public Subtype changeStatus(String newStatus, String byNullable) {
        if (!Objects.equals(newStatus, "A") && !Objects.equals(newStatus, "I"))
            throw new IllegalArgumentException("status debe ser 'A' o 'I'");
        return new Subtype(
                this.subtypeCode, this.bin, this.name, this.description, newStatus,
                this.ownerIdType, this.ownerIdNumber, this.binExt, this.binEfectivo, this.subtypeId,
                this.createdAt, OffsetDateTime.now(), byNullable
        );
    }

    public static Subtype rehydrate(String subtypeCode, String bin, String name, String description, String status,
                                    String ownerIdType, String ownerIdNumber, String binExt, String binEfectivo,
                                    Long subtypeId, OffsetDateTime createdAt, OffsetDateTime updatedAt, String updatedBy) {
        return new Subtype(subtypeCode, bin, name, description, status,
                ownerIdType, ownerIdNumber, binExt, binEfectivo, subtypeId, createdAt, updatedAt, updatedBy);
    }

    private static String digitsOnly(String s) { return s == null ? null : s.replaceAll("\\D", ""); }
    private static String digitsOnlyOrNull(String s) {
        if (s == null) return null;
        String d = s.replaceAll("\\D", "");
        return d.isBlank() ? null : d;
    }


    public static String computeBinEfectivo(String bin, String ext) {
        return bin + (ext == null ? "" : ext);
    }

    private static void require(String v, String f) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " es requerido");
    }
}

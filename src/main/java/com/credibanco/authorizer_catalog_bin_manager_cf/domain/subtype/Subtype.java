package com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

public record Subtype(
        String subtypeCode,   // 3 chars
        String bin,           // 6/8/9 dígitos (normalizado: solo números)
        String name,          // requerido
        String description,   // opcional
        String status,        // 'A' | 'I'
        String ownerIdType,   // FK opcional
        String ownerIdNumber, // opcional
        String binExt,        // 6->3 díg, 8->1 díg, 9->null
        String binEfectivo,   // 9 díg, derivado de bin/binExt
        Long   subtypeId,     // asignado por secuencia en BD
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {
    public Subtype {
        require(subtypeCode, "subtypeCode");
        if (subtypeCode.length() != 3) throw new IllegalArgumentException("subtypeCode debe tener 3 caracteres");

        require(bin, "bin");
        if (!bin.chars().allMatch(Character::isDigit)) throw new IllegalArgumentException("bin debe ser numérico");
        final int len = bin.length();
        if (len != 6 && len != 8 && len != 9) throw new IllegalArgumentException("bin debe ser de 6, 8 o 9 dígitos");

        require(name, "name");

        require(status, "status");
        if (!Objects.equals(status, "A") && !Objects.equals(status, "I"))
            throw new IllegalArgumentException("status debe ser 'A' o 'I'");

        // Regla 6+3, 8+1, 9+0 (CK_SUBTYPE_BIN_EXT_UNIFIED)
        if (len == 6) {
            if (binExt == null || !binExt.chars().allMatch(Character::isDigit) || binExt.length() != 3)
                throw new IllegalArgumentException("para BIN(6) binExt debe ser 3 dígitos");
        } else if (len == 8) {
            if (binExt == null || !binExt.chars().allMatch(Character::isDigit) || binExt.length() != 1)
                throw new IllegalArgumentException("para BIN(8) binExt debe ser 1 dígito");
        } else { // len == 9
            if (binExt != null) throw new IllegalArgumentException("BIN(9) no admite binExt");
        }

        // binEfectivo derivado (TRG_SUBTYPE_BIN_EFECTIVO)
        var expectedEff = computeBinEfectivo(bin, binExt);
        if (binEfectivo == null || !binEfectivo.equals(expectedEff))
            throw new IllegalArgumentException("binEfectivo inconsistente con bin/binExt");
    }

    public static Subtype createNew(String subtypeCode, String rawBin, String name,
                                    String description, String ownerIdType, String ownerIdNumber,
                                    String rawBinExt, String createdBy) {
        String normBin = digitsOnly(rawBin);
        String normExt = normalizeExt(normBin, rawBinExt);
        String eff     = computeBinEfectivo(normBin, normExt);
        var now = LocalDateTime.now();
        return new Subtype(
                subtypeCode, normBin, name, description, "I",
                ownerIdType, ownerIdNumber, normExt, eff, null, null, null, createdBy
        );
    }

    /** Actualizar datos editables (no PK). */
    public Subtype updateBasics(String newName, String newDescription,
                                String newOwnerIdType, String newOwnerIdNumber,
                                String newRawBinExt, String by) {
        require(newName, "name");
        String normExt = normalizeExt(this.bin, newRawBinExt);
        String eff     = computeBinEfectivo(this.bin, normExt);
        return new Subtype(
                this.subtypeCode, this.bin, newName, newDescription, this.status,
                newOwnerIdType, newOwnerIdNumber, normExt, eff, this.subtypeId,
                this.createdAt, OffsetDateTime.now(), by
        );
    }

    /** Cambiar estado. Al activar se validará la regla de agencias activas. */
    public Subtype changeStatus(String newStatus, String by) {
        if (!Objects.equals(newStatus, "A") && !Objects.equals(newStatus, "I"))
            throw new IllegalArgumentException("status debe ser 'A' o 'I'");
        return new Subtype(
                this.subtypeCode, this.bin, this.name, this.description, newStatus,
                this.ownerIdType, this.ownerIdNumber, this.binExt, this.binEfectivo, this.subtypeId,
                this.createdAt, OffsetDateTime.now(), by
        );
    }

    /** Rehidratación desde BD. */
    public static Subtype rehydrate(String subtypeCode, String bin, String name, String description, String status,
                                    String ownerIdType, String ownerIdNumber, String binExt, String binEfectivo,
                                    Long subtypeId, OffsetDateTime createdAt, OffsetDateTime updatedAt, String updatedBy) {
        return new Subtype(subtypeCode, bin, name, description, status,
                ownerIdType, ownerIdNumber, binExt, binEfectivo, subtypeId, createdAt, updatedAt, updatedBy);
    }

    private static String digitsOnly(String s) { return s == null ? null : s.replaceAll("\\D", ""); }

    private static String normalizeExt(String bin, String rawExt) {
        int len = bin.length();
        if (len == 6) {
            String e = digitsOnly(rawExt);
            if (e == null) throw new IllegalArgumentException("para BIN(6) binExt requerido");
            if (e.length() > 3) e = e.substring(0,3);
            return String.format("%03d", Integer.parseInt(e)); // LPAD 3
        } else if (len == 8) {
            String e = digitsOnly(rawExt);
            if (e == null || e.isEmpty()) throw new IllegalArgumentException("para BIN(8) binExt requerido");
            return e.substring(0,1); // 1 díg
        } else { // 9
            if (rawExt != null) throw new IllegalArgumentException("BIN(9) no admite binExt");
            return null;
        }
    }

    /** Reglas de trigger TRG_SUBTYPE_BIN_EFECTIVO. */
    public static String computeBinEfectivo(String bin, String ext) {
        int len = bin.length();
        if (len == 6 && ext != null) return (bin + ext).substring(0, 9);
        if (len == 8 && ext != null) return (bin + ext.substring(0,1)).substring(0, 9);
        if (len >= 9)                return bin.substring(0, 9);
        return null;
    }

    private static void require(String v, String f) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " es requerido");
    }
}
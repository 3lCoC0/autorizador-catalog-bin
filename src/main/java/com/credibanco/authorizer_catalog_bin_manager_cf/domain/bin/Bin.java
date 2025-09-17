package com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

public record Bin(
        String bin,               // 6/8/9 dígitos
        String name,
        String typeBin,           // DEBITO|CREDITO|PREPAGO
        String typeAccount,       // 2 dígitos
        String compensationCod,   // opcional
        String description,       // opcional
        String status,            // A|I
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {
    public Bin {
        require(bin, "bin");
        if (!bin.chars().allMatch(Character::isDigit)) throw new IllegalArgumentException("BIN debe ser numérico");
        int len = bin.length();
        if (len != 6 && len != 8 && len != 9) throw new IllegalArgumentException("BIN debe ser de 6, 8 o 9 dígitos");

        require(name, "name");
        require(typeBin, "typeBin");
        if (!("DEBITO".equals(typeBin) || "CREDITO".equals(typeBin) || "PREPAGO".equals(typeBin)))
            throw new IllegalArgumentException("typeBin inválido (use DEBITO|CREDITO|PREPAGO)");

        require(typeAccount, "typeAccount");
        if (!(typeAccount.length() == 2 && typeAccount.chars().allMatch(Character::isDigit)))
            throw new IllegalArgumentException("typeAccount debe ser de 2 dígitos");

        require(status, "status");
        if (!Objects.equals(status, "A") && !Objects.equals(status, "I"))
            throw new IllegalArgumentException("status debe ser 'A' o 'I'");
    }

    /** Creación (DDD): estado A y timestamps. */
    public static Bin createNew(String bin, String name, String typeBin, String typeAccount,
                                String compensationCod, String description, String createdBy) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        return new Bin(bin, name, typeBin, typeAccount, compensationCod, description, "A", now, now, createdBy);
    }

    /** Rehidratación desde DB. */
    public static Bin rehydrate(String bin, String name, String typeBin, String typeAccount,
                                String compensationCod, String description, String status,
                                OffsetDateTime  createdAt, OffsetDateTime  updatedAt, String updatedBy) {
        return new Bin(bin, name, typeBin, typeAccount, compensationCod, description, status,
                createdAt, updatedAt, updatedBy);
    }

    /** Cambiar estado (inmutable). */
    public Bin changeStatus(String newStatus, String by) {
        if (!Objects.equals(newStatus, "A") && !Objects.equals(newStatus, "I"))
            throw new IllegalArgumentException("status debe ser 'A' o 'I'");
        return new Bin(bin, name, typeBin, typeAccount, compensationCod, description,
                newStatus, createdAt, OffsetDateTime.now(ZoneOffset.UTC), by);
    }

    /** Actualizar datos básicos (inmutable). */
    public Bin updateBasics(String newName, String newTypeBin, String newTypeAccount,
                            String newCompCod, String newDescription, String by) {
        require(newName, "name");
        require(newTypeBin, "typeBin");
        if (!("DEBITO".equals(newTypeBin) || "CREDITO".equals(newTypeBin) || "PREPAGO".equals(newTypeBin)))
            throw new IllegalArgumentException("typeBin inválido (use DEBITO|CREDITO|PREPAGO)");
        require(newTypeAccount, "typeAccount");
        if (!(newTypeAccount.length() == 2 && newTypeAccount.chars().allMatch(Character::isDigit)))
            throw new IllegalArgumentException("typeAccount debe ser de 2 dígitos");

        return new Bin(bin, newName, newTypeBin, newTypeAccount, newCompCod, newDescription,
                status, createdAt, OffsetDateTime.now(ZoneOffset.UTC), by);
    }

    private static void require(String v, String f) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " es requerido");
    }
}
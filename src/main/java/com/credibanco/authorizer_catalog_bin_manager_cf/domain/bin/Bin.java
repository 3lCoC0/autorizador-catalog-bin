package com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

public record Bin(
        String bin,
        String name,
        String typeBin,
        String typeAccount,
        String compensationCod,
        String description,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy,          // <-- puede ser null
        String usesBinExt,         // <-- 'Y'|'N'
        Integer binExtDigits    // <-- 1|2|3 si usesBinExt='Y', si 'N' => null
) {
    private static final Pattern ALPHANUMERIC_WITH_SPACES = Pattern.compile("^[\\p{L}\\p{N}\\s]+$");

    public Bin {
        require(bin, "bin");
        if (!bin.chars().allMatch(Character::isDigit)) throw new IllegalArgumentException("BIN debe ser numérico");
        int len = bin.length();
        if (len < 6 || len > 9) throw new IllegalArgumentException("BIN debe tener entre 6 y 9 dígitos");

        require(name, "name");
        ensureNoSpecialCharacters(name, "name");
        require(typeBin, "typeBin");
        if (!("DEBITO".equals(typeBin) || "CREDITO".equals(typeBin) || "PREPAGO".equals(typeBin)))
            throw new IllegalArgumentException("typeBin inválido (use DEBITO|CREDITO|PREPAGO)");

        require(typeAccount, "typeAccount");
        if (!(typeAccount.length() == 2 && typeAccount.chars().allMatch(Character::isDigit)))
            throw new IllegalArgumentException("typeAccount debe ser de 2 dígitos");

        ensureNoSpecialCharacters(compensationCod, "compensationCod");
        ensureNoSpecialCharacters(description, "description");

        require(status, "status");
        if (!Objects.equals(status, "A") && !Objects.equals(status, "I"))
            throw new IllegalArgumentException("status debe ser 'A' o 'I'");


        require(usesBinExt, "usesBinExt");
        if (!("Y".equals(usesBinExt) || "N".equals(usesBinExt)))
            throw new IllegalArgumentException("usesBinExt debe ser 'Y' o 'N'");


        int baseLen = bin.length();
        if ("Y".equals(usesBinExt)) {
            if (binExtDigits == null || !(binExtDigits == 1 || binExtDigits == 2 || binExtDigits == 3)) {
                throw new IllegalArgumentException("binExtDigits debe ser 1, 2 o 3 cuando usesBinExt='Y'");
            }
            if (baseLen + binExtDigits > 9) {
                throw new IllegalArgumentException("Entre el bin base y su extendido no se pueden usar mas de 9 digitos");
            }
        } else {
            if (binExtDigits != null) {
                throw new IllegalArgumentException("binExtDigits debe ser null cuando usesBinExt='N'");
            }
        }
    }


        public static Bin createNew(String bin, String name, String typeBin, String typeAccount,
                                String compensationCod, String description,
                                String usesBinExt, Integer binExtDigits,
                                String createdByNullable) {
        var now = OffsetDateTime.now();
        return new Bin(bin, name, typeBin, typeAccount, compensationCod, description,
                "A", now, now, createdByNullable, usesBinExt, binExtDigits);
    }

    public static Bin rehydrate(String bin, String name, String typeBin, String typeAccount,
                                String compensationCod, String description, String status,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt, String updatedBy,
                                String usesBinExt, Integer binExtDigits) {
        return new Bin(bin, name, typeBin, typeAccount, compensationCod, description,
                status, createdAt, updatedAt, updatedBy, usesBinExt, binExtDigits);
    }

    public Bin changeStatus(String newStatus, String byNullable) {
        if (!Objects.equals(newStatus, "A") && !Objects.equals(newStatus, "I"))
            throw new IllegalArgumentException("status debe ser 'A' o 'I'");
        return new Bin(bin, name, typeBin, typeAccount, compensationCod, description,
                newStatus, createdAt, OffsetDateTime.now(), byNullable, usesBinExt, binExtDigits);
    }

    public Bin updateBasics(String newName, String newTypeBin, String newTypeAccount,
                            String newCompCod, String newDescription,
                            String newUsesBinExt, Integer newBinExtDigits,
                            String byNullable) {
        require(newName, "name");
        ensureNoSpecialCharacters(newName, "name");
        require(newTypeBin, "typeBin");
        if (!("DEBITO".equals(newTypeBin) || "CREDITO".equals(newTypeBin) || "PREPAGO".equals(newTypeBin)))
            throw new IllegalArgumentException("typeBin inválido (use DEBITO|CREDITO|PREPAGO)");
        require(newTypeAccount, "typeAccount");
        if (!(newTypeAccount.length() == 2 && newTypeAccount.chars().allMatch(Character::isDigit)))
            throw new IllegalArgumentException("typeAccount debe ser de 2 dígitos");


        ensureNoSpecialCharacters(newCompCod, "compensationCod");
        ensureNoSpecialCharacters(newDescription, "description");


        int baseLen = bin.length();
        if ("Y".equals(usesBinExt)) {
            if (binExtDigits == null || !(binExtDigits == 1 || binExtDigits == 2 || binExtDigits == 3)) {
                throw new IllegalArgumentException("binExtDigits debe ser 1, 2 o 3 cuando usesBinExt='Y'");
            }
            if (baseLen + binExtDigits > 9) {
                throw new IllegalArgumentException("Entre el bin base y su extendido no se pueden usar mas de 9 digitos");
            }
        } else {
            if (binExtDigits != null) {
                throw new IllegalArgumentException("binExtDigits debe ser null cuando usesBinExt='N'");
            }
        }


        return new Bin(bin, newName, newTypeBin, newTypeAccount, newCompCod, newDescription,
                status, createdAt, OffsetDateTime.now(), byNullable, newUsesBinExt, newBinExtDigits);
    }

    private static void require(String v, String f) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " es requerido");
    }

    private static void ensureNoSpecialCharacters(String value, String field) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!ALPHANUMERIC_WITH_SPACES.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " no debe contener caracteres especiales");
        }
    }
}

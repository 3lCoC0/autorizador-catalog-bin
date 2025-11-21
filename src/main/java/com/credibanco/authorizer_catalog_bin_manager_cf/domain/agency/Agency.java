package com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency;

import java.time.OffsetDateTime;
import java.util.Objects;

public record Agency(
        String subtypeCode,
        String agencyCode,
        String name,
        String agencyNit,
        String address,
        String phone,
        String municipalityDaneCode,
        String embosserHighlight,
        String embosserPins,
        String cardCustodianPrimary,
        String cardCustodianPrimaryId,
        String cardCustodianSecondary,
        String cardCustodianSecondaryId,
        String pinCustodianPrimary,
        String pinCustodianPrimaryId,
        String pinCustodianSecondary,
        String pinCustodianSecondaryId,
        String description,
        String status, // 'A'|'I'
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {

    public Agency {
        require(subtypeCode, "subtypeCode");
        require(agencyCode, "agencyCode");
        require(name, "name");
        require(status, "status");
        if (!Objects.equals(status, "A") && !Objects.equals(status, "I"))
            throw new IllegalArgumentException("status debe ser 'A' o 'I'");
    }

    public static Agency createNew(String subtypeCode, String agencyCode, String name,
                                   String agencyNit, String address, String phone, String municipalityDaneCode,
                                   String embosserHighlight, String embosserPins,
                                   String cardCustodianPrimary, String cardCustodianPrimaryId,
                                   String cardCustodianSecondary, String cardCustodianSecondaryId,
                                   String pinCustodianPrimary, String pinCustodianPrimaryId,
                                   String pinCustodianSecondary, String pinCustodianSecondaryId,
                                   String description, String createdByNullable) {
        var now = OffsetDateTime.now();
        return new Agency(
                subtypeCode, agencyCode, name,
                agencyNit, address, phone, municipalityDaneCode,
                embosserHighlight, embosserPins,
                cardCustodianPrimary, cardCustodianPrimaryId,
                cardCustodianSecondary, cardCustodianSecondaryId,
                pinCustodianPrimary, pinCustodianPrimaryId,
                pinCustodianSecondary, pinCustodianSecondaryId,
                description, "A", now, now, createdByNullable
        );
    }

    public static Agency rehydrate(String subtypeCode, String agencyCode, String name,
                                   String agencyNit, String address, String phone, String municipalityDaneCode,
                                   String embosserHighlight, String embosserPins,
                                   String cardCustodianPrimary, String cardCustodianPrimaryId,
                                   String cardCustodianSecondary, String cardCustodianSecondaryId,
                                   String pinCustodianPrimary, String pinCustodianPrimaryId,
                                   String pinCustodianSecondary, String pinCustodianSecondaryId,
                                   String description, String status,
                                   OffsetDateTime createdAt, OffsetDateTime updatedAt, String updatedBy) {
        return new Agency(
                subtypeCode, agencyCode, name,
                agencyNit, address, phone, municipalityDaneCode,
                embosserHighlight, embosserPins,
                cardCustodianPrimary, cardCustodianPrimaryId,
                cardCustodianSecondary, cardCustodianSecondaryId,
                pinCustodianPrimary, pinCustodianPrimaryId,
                pinCustodianSecondary, pinCustodianSecondaryId,
                description, status, createdAt, updatedAt, updatedBy
        );
    }

    public Agency updateBasics(String name, String agencyNit, String address, String phone, String municipalityDaneCode,
                               String embosserHighlight, String embosserPins,
                               String cardCustodianPrimary, String cardCustodianPrimaryId,
                               String cardCustodianSecondary, String cardCustodianSecondaryId,
                               String pinCustodianPrimary, String pinCustodianPrimaryId,
                               String pinCustodianSecondary, String pinCustodianSecondaryId,
                               String description, String byNullable) {
        require(name, "name");
        return new Agency(
                subtypeCode, agencyCode, name,
                agencyNit, address, phone, municipalityDaneCode,
                embosserHighlight, embosserPins,
                cardCustodianPrimary, cardCustodianPrimaryId,
                cardCustodianSecondary, cardCustodianSecondaryId,
                pinCustodianPrimary, pinCustodianPrimaryId,
                pinCustodianSecondary, pinCustodianSecondaryId,
                description, status, createdAt, OffsetDateTime.now(), byNullable
        );
    }

    public Agency changeStatus(String newStatus, String byNullable) {
        if (!Objects.equals(newStatus, "A") && !Objects.equals(newStatus, "I"))
            throw new IllegalArgumentException("status debe ser 'A' o 'I'");
        return new Agency(
                subtypeCode, agencyCode, name,
                agencyNit, address, phone, municipalityDaneCode,
                embosserHighlight, embosserPins,
                cardCustodianPrimary, cardCustodianPrimaryId,
                cardCustodianSecondary, cardCustodianSecondaryId,
                pinCustodianPrimary, pinCustodianPrimaryId,
                pinCustodianSecondary, pinCustodianSecondaryId,
                description, newStatus, createdAt, OffsetDateTime.now(), byNullable
        );
    }

    private static void require(String v, String f) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " es requerido");
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto;

import java.time.OffsetDateTime;

public record AgencyResponse(
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
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String updatedBy
) {}

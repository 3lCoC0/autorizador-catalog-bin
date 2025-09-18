package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgencyCreateRequest(
        @NotBlank @Size(max=3)  String subtypeCode,
        @NotBlank @Size(max=20) String agencyCode,
        @NotBlank @Size(max=120) String name,
        @Size(max=20)  String agencyNit,
        @Size(max=200) String address,
        @Size(max=30)  String phone,
        @Size(max=10)  String municipalityDaneCode,
        @Size(max=120) String embosserHighlight,
        @Size(max=120) String embosserPins,
        @Size(max=120) String cardCustodianPrimary,
        @Size(max=30)  String cardCustodianPrimaryId,
        @Size(max=120) String cardCustodianSecondary,
        @Size(max=30)  String cardCustodianSecondaryId,
        @Size(max=120) String pinCustodianPrimary,
        @Size(max=30)  String pinCustodianPrimaryId,
        @Size(max=120) String pinCustodianSecondary,
        @Size(max=30)  String pinCustodianSecondaryId,
        @Size(max=400) String description,
        @NotBlank String createdBy
) {}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AgencyCreateRequest(
        @NotBlank @Size(max=3)  @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "subtypeCode no debe contener caracteres especiales") String subtypeCode,
        @NotBlank @Size(max=2) @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "agencyCode no debe contener caracteres especiales") String agencyCode,
        @NotBlank @Size(max=120) @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "name no debe contener caracteres especiales") String name,
        @NotBlank @Size(max=20)  @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "agencyNit no debe contener caracteres especiales") String agencyNit,
        @Size(max=200) String address,
        @Size(max=30)  @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "phone no debe contener caracteres especiales") String phone,
        @Size(max=10)  @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "municipalityDaneCode no debe contener caracteres especiales") String municipalityDaneCode,
        @Size(max=120) @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "embosserHighlight no debe contener caracteres especiales") String embosserHighlight,
        @Size(max=120) @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "embosserPins no debe contener caracteres especiales") String embosserPins,
        @Size(max=120) @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "cardCustodianPrimary no debe contener caracteres especiales") String cardCustodianPrimary,
        @Size(max=30)  @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "cardCustodianPrimaryId no debe contener caracteres especiales") String cardCustodianPrimaryId,
        @Size(max=120) @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "cardCustodianSecondary no debe contener caracteres especiales") String cardCustodianSecondary,
        @Size(max=30)  @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "cardCustodianSecondaryId no debe contener caracteres especiales") String cardCustodianSecondaryId,
        @Size(max=120) @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "pinCustodianPrimary no debe contener caracteres especiales") String pinCustodianPrimary,
        @Size(max=30)  @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "pinCustodianPrimaryId no debe contener caracteres especiales") String pinCustodianPrimaryId,
        @Size(max=120) @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "pinCustodianSecondary no debe contener caracteres especiales") String pinCustodianSecondary,
        @Size(max=30)  @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "pinCustodianSecondaryId no debe contener caracteres especiales") String pinCustodianSecondaryId,
        @Size(max=400) @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "description no debe contener caracteres especiales") String description,
        String createdBy
) {}

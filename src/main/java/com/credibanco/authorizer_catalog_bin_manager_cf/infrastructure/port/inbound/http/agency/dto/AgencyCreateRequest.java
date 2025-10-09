package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.AlphaNumericWithSpaces;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AgencyCreateRequest(
        @NotBlank @Size(max=3)  @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "subtypeCode no debe contener caracteres especiales") String subtypeCode,
        @NotBlank @Size(max=2) @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "agencyCode no debe contener caracteres especiales") String agencyCode,
        @NotBlank @Size(max=120) @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "name no debe contener caracteres especiales") String name,
        @Size(max=20)  @AlphaNumericWithSpaces(message = "agencyNit no debe contener caracteres especiales") String agencyNit,
        @Size(max=200) @AlphaNumericWithSpaces(message = "address no debe contener caracteres especiales") String address,
        @Size(max=30)  @AlphaNumericWithSpaces(message = "phone no debe contener caracteres especiales") String phone,
        @Size(max=10)  @AlphaNumericWithSpaces(message = "municipalityDaneCode no debe contener caracteres especiales") String municipalityDaneCode,
        @Size(max=120) @AlphaNumericWithSpaces(message = "embosserHighlight no debe contener caracteres especiales") String embosserHighlight,
        @Size(max=120) @AlphaNumericWithSpaces(message = "embosserPins no debe contener caracteres especiales") String embosserPins,
        @Size(max=120) @AlphaNumericWithSpaces(message = "cardCustodianPrimary no debe contener caracteres especiales") String cardCustodianPrimary,
        @Size(max=30)  @AlphaNumericWithSpaces(message = "cardCustodianPrimaryId no debe contener caracteres especiales") String cardCustodianPrimaryId,
        @Size(max=120) @AlphaNumericWithSpaces(message = "cardCustodianSecondary no debe contener caracteres especiales") String cardCustodianSecondary,
        @Size(max=30)  @AlphaNumericWithSpaces(message = "cardCustodianSecondaryId no debe contener caracteres especiales") String cardCustodianSecondaryId,
        @Size(max=120) @AlphaNumericWithSpaces(message = "pinCustodianPrimary no debe contener caracteres especiales") String pinCustodianPrimary,
        @Size(max=30)  @AlphaNumericWithSpaces(message = "pinCustodianPrimaryId no debe contener caracteres especiales") String pinCustodianPrimaryId,
        @Size(max=120) @AlphaNumericWithSpaces(message = "pinCustodianSecondary no debe contener caracteres especiales") String pinCustodianSecondary,
        @Size(max=30)  @AlphaNumericWithSpaces(message = "pinCustodianSecondaryId no debe contener caracteres especiales") String pinCustodianSecondaryId,
        @Size(max=400) @AlphaNumericWithSpaces(message = "description no debe contener caracteres especiales") String description,
        @AlphaNumericWithSpaces(message = "createdBy no debe contener caracteres especiales") String createdBy
) {}

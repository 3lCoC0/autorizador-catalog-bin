package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.agency.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.jackson.BlankAsNull;
import com.credibanco.authorizer_catalog_bin_manager_cf.shared.validation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


public record AgencyUpdateRequest(
        @NotBlank(message = "name no puede ser vacio")
        @NotNull(message = "name no puede ser nulo")
        @SafeText(allowNumbers = true, allowUnderscore = false, allowSpaces = true,
                message = "name solo permite letras, numeros (incluye tildes/ñ) y espacios; no se admite la palabra 'null' ni caracteres especiales, no debe estar vacio")
        String name,


        @NotNull(message = "agencyNit no puede ser nulo")
        @NotBlank(message = "agencyNit no puede ser vacio")
        @Pattern(regexp="\\d*", message="agencyNit debe ser numérico")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "agencyNit no debe contener caracteres especiales") String agencyNit,

        @BlankAsNull
        String address,

        @BlankAsNull
        @Pattern(regexp="\\d*", message="phone debe ser numérico")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "phone no debe contener caracteres especiales") String phone,


        @BlankAsNull
        @Pattern(regexp="\\d*", message="municipalityDaneCode debe ser numérico")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "municipalityDaneCode no debe contener caracteres especiales") String municipalityDaneCode,

        @BlankAsNull
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "embosserHighlight no debe contener caracteres especiales") String embosserHighlight,


        @BlankAsNull
        @Pattern(regexp="\\d*", message="embosserPins debe ser numérico")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "embosserPins no debe contener caracteres especiales") String embosserPins,



        @BlankAsNull
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "cardCustodianPrimary no debe contener caracteres especiales") String cardCustodianPrimary,

        @BlankAsNull
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "cardCustodianPrimaryId no debe contener caracteres especiales") String cardCustodianPrimaryId,

        @BlankAsNull
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "cardCustodianSecondary no debe contener caracteres especiales") String cardCustodianSecondary,

        @BlankAsNull
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "cardCustodianSecondaryId no debe contener caracteres especiales") String cardCustodianSecondaryId,

        @BlankAsNull
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "pinCustodianPrimary no debe contener caracteres especiales") String pinCustodianPrimary,

        @BlankAsNull
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "pinCustodianPrimaryId no debe contener caracteres especiales") String pinCustodianPrimaryId,


        @BlankAsNull
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "pinCustodianSecondary no debe contener caracteres especiales") String pinCustodianSecondary,

        @BlankAsNull
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "pinCustodianSecondaryId no debe contener caracteres especiales") String pinCustodianSecondaryId,

        @BlankAsNull
        @SafeText(allowNumbers = true, allowUnderscore = false, allowSpaces = true,
                message = "description solo permite letras, numeros (incluye tildes/ñ) y espacios; no se admite la palabra 'null' ni caracteres especiales")
        String description,

        String updatedBy
) {}

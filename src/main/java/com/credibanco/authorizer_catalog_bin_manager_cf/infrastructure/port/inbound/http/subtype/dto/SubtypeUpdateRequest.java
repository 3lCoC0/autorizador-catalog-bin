package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.jackson.BlankAsNull;
import com.credibanco.authorizer_catalog_bin_manager_cf.shared.validation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SubtypeUpdateRequest(
        @NotNull(message = "name no puede ser nulo")
        @SafeText(allowNumbers = true, allowUnderscore = false, allowSpaces = true,
                message = "name solo permite letras, numeros (incluye tildes/ñ) y espacios; no se admite la palabra 'null' ni caracteres especiales, no debe estar vacio")
        String name,

        @BlankAsNull
        @SafeText(allowNumbers = true, allowUnderscore = false, allowSpaces = true,
                message = "description solo permite letras, numeros (incluye tildes/ñ) y espacios; no se admite la palabra 'null' ni caracteres especiales")
        String description,

        @NotNull(message = "ownerIdType no puede ser nulo")
        @NotBlank(message = "ownerIdType no puede ser vacio")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "ownerIdType no debe contener caracteres especiales") String ownerIdType,


        @NotNull(message = "ownerIdNumber no puede ser nulo")
        @NotBlank(message = "ownerIdNumber no puede ser vacio")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "ownerIdNumber no debe contener caracteres especiales")
        @Pattern(regexp="\\d*", message="ownerIdNumber debe ser numérico") String ownerIdNumber,


        @BlankAsNull
        @Pattern(regexp="\\d*", message="binExt debe ser numérico, o dejar el campo en blanco cuando no tiene ext") String binExt,

        String updatedBy
) {}
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation.AlphaNumericWithSpaces;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SubtypeUpdateRequest(
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "name no debe contener caracteres especiales") String name,
        @AlphaNumericWithSpaces(message = "description no debe contener caracteres especiales") String description,
        @AlphaNumericWithSpaces(message = "ownerIdType no debe contener caracteres especiales") String ownerIdType,
        @AlphaNumericWithSpaces(message = "ownerIdNumber no debe contener caracteres especiales") String ownerIdNumber,
        @Pattern(regexp="\\d*", message="binExt debe ser numérico") String binExt,
        @AlphaNumericWithSpaces(message = "updatedBy no debe contener caracteres especiales") String updatedBy
) {}
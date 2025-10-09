package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SubtypeUpdateRequest(
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "name no debe contener caracteres especiales") String name,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "description no debe contener caracteres especiales") String description,
        @NotBlank@Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "ownerIdType no debe contener caracteres especiales") String ownerIdType,
        @NotBlank@Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "ownerIdNumber no debe contener caracteres especiales")
        @Pattern(regexp="\\d*", message="ownerIdNumber debe ser numérico") String ownerIdNumber,
        @Pattern(regexp="\\d*", message="binExt debe ser numérico") String binExt,
        String updatedBy
) {}
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubtypeCreateRequest(
        @NotBlank  @Size(min = 3, max = 3,message="subtypeCode debe ser de longitud de 3")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "name no debe contener caracteres especiales") String subtypeCode,
        @NotBlank @Pattern(regexp="\\d{6,9}", message="bin debe ser numérico de longitud entre 6 y 9 posiciones") String bin,
        @NotBlank @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "name no debe contener caracteres especiales") String name,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "description no debe contener caracteres especiales") String description,
        @NotBlank@Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "ownerIdType no debe contener caracteres especiales") String ownerIdType,
        @NotBlank@Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "ownerIdNumber no debe contener caracteres especiales") String ownerIdNumber,
        @Pattern(regexp="\\d*", message="binExt debe ser numérico") String binExt,
        String createdBy
) {}
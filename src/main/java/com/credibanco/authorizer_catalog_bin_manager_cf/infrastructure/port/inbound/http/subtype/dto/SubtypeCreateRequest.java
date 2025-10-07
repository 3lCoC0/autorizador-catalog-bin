package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubtypeCreateRequest(
        @NotBlank @Size(min = 3, max = 3,message="subtypeCode debe ser de longitud de 3") String subtypeCode,
        @NotBlank @Pattern(regexp="\\d{6,9}", message="bin debe ser numérico de longitud entre 6 y 9 posiciones") String bin,
        @NotBlank String name,
        String description,
        String ownerIdType,
        String ownerIdNumber,
        @Pattern(regexp="\\d*", message="binExt debe ser numérico") String binExt,
        String createdBy
) {}
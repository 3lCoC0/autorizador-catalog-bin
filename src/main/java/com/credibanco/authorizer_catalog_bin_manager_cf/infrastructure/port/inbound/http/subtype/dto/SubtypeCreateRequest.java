package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubtypeCreateRequest(
        @NotBlank @Size(min = 3, max = 3) String subtypeCode,
        @NotBlank @Pattern(regexp="\\d{6}|\\d{8}|\\d{9}") String bin,
        @NotBlank String name,
        String description,
        String ownerIdType,
        String ownerIdNumber,
        @Pattern(regexp="\\d*", message="binExt debe ser numérico") String binExt,
        @NotBlank String createdBy
) {}

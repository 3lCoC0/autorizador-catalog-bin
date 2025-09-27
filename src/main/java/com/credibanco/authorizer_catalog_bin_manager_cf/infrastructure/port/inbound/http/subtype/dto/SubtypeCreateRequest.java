package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.subtype.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubtypeCreateRequest(
        @NotBlank @Size(min = 3, max = 3) String subtypeCode,
        @NotBlank @Pattern(regexp="\\d{6,9}") String bin,     // ahora 6..9
        @NotBlank String name,
        String description,
        String ownerIdType,
        String ownerIdNumber,
        @Pattern(regexp="\\d*", message="binExt debe ser numérico") String binExt,
        String createdBy
) {}
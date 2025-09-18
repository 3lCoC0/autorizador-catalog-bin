package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record BinCreateRequest(
        @NotBlank @Pattern(regexp="\\d{6}|\\d{8}|\\d{9}") String bin,
        @NotBlank String name,
        String typeBin,
        String typeAccount,
        String compensationCod,
        String description,
        @NotBlank String createdBy
) {}
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BinUpdateRequest(
        @NotBlank @Pattern(regexp="\\d{6,9}", message="bin debe tener entre 6 y 9 dígitos")
        String bin,
        @NotBlank @Size(min=3, max=120) String name,
        @NotBlank @Pattern(regexp="DEBITO|CREDITO|PREPAGO",message="typeBin debe ser DEBITO|CREDITO|PREPAGO") String typeBin,
        @NotBlank @Pattern(regexp="\\d{2}",message="typeAccount debe ser de 2 posiciones") String typeAccount,
        String compensationCod,
        String description,
        @NotBlank @Pattern(regexp="Y|N", message="usesBinExt debe ser 'Y' o 'N'") String usesBinExt,
        Integer binExtDigits,
        String updatedBy
) {}
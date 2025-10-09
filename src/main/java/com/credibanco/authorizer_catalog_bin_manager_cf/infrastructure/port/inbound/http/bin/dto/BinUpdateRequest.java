package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BinUpdateRequest(
        @NotBlank @Pattern(regexp="\\d{6,9}", message="bin debe ser numérico de longitud entre 6 y 9 posiciones") String bin,
        @NotBlank
        @Size(min=3, max=120)
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "name no debe contener caracteres especiales")
        String name,
        @NotBlank @Pattern(regexp="DEBITO|CREDITO|PREPAGO",message="typeBin debe ser DEBITO|CREDITO|PREPAGO") String typeBin,
        @NotBlank @Pattern(regexp="\\d{2}",message="typeAccount debe ser de 2 posiciones numericas") String typeAccount,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "compensationCod no debe contener caracteres especiales")
        String compensationCod,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "description no debe contener caracteres especiales")
        String description,
        @NotBlank @Pattern(regexp= "[YN]", message="usesBinExt debe ser 'Y' o 'N'") String usesBinExt,
        Integer binExtDigits,
        String updatedBy
) {}
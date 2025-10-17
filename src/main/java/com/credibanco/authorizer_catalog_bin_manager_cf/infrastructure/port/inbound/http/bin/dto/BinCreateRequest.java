package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.jackson.BlankAsNull;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.jackson.NullOrBlankToNullIntegerDeserializer;
import com.credibanco.authorizer_catalog_bin_manager_cf.shared.validation.SafeText;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;


public record BinCreateRequest(
        @NotBlank @Pattern(regexp="\\d{6,9}", message="bin debe ser numérico de longitud entre 6 y 9 posiciones") String bin,
        @NotBlank
        @NotNull(message = "name no puede ser nulo")
        @SafeText(allowNumbers = true, allowUnderscore = false, allowSpaces = true,
                message = "name solo permite letras, numeros (incluye tildes/ñ) y espacios; no se admite la palabra 'null' ni caracteres especiales")
        String name,
        @NotNull
        @NotBlank @Pattern(regexp="DEBITO|CREDITO|PREPAGO",message="typeBin debe ser DEBITO|CREDITO|PREPAGO") String typeBin,
        @NotNull @Pattern(regexp="\\d{2}",message="typeAccount debe ser de 2 posiciones numericas") String typeAccount,
        @SafeText(allowNumbers = true, allowUnderscore = false, allowSpaces = true,
                message = "compensationCod solo permite letras y numeros no se admite la palabra 'null'")
        String compensationCod,
        @BlankAsNull @SafeText(allowNumbers = true, allowUnderscore = false, allowSpaces = true,
                message = "description solo permite letras, numeros (incluye tildes/ñ) y espacios; no se admite la palabra 'null'")
        String description,
        @NotBlank @Pattern(regexp= "[YN]", message="usesBinExt debe ser 'Y' o 'N'") String usesBinExt,
        String createdBy,
        @JsonDeserialize(using = NullOrBlankToNullIntegerDeserializer.class)
        @Min(value = 1, message = "binExtDigits solo permite los numeros 1, 2 o 3 (o la palabra null cuando usesBinExt es igual a N).")
        @Max(value = 3, message = "binExtDigits solo permite los numeros 1, 2 o 3 (o la palabra null cuando usesBinExt es igual a N).")
                Integer binExtDigits

) {}
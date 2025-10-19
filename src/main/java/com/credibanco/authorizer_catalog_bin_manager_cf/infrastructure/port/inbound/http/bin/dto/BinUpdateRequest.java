package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.bin.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.jackson.BlankAsNull;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.jackson.NullOrBlankToNullIntegerDeserializer;
import com.credibanco.authorizer_catalog_bin_manager_cf.shared.validation.SafeText;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;

public record BinUpdateRequest(
        @NotNull(message = "bin no puede ser nulo")
        @Pattern(regexp="\\d{6,9}", message="bin debe ser numérico de longitud entre 6 y 9 posiciones") String bin,


        @NotNull(message = "name no puede ser nulo")
        @SafeText(allowNumbers = true, allowUnderscore = false, allowSpaces = true,
                message = "name solo permite letras, numeros (incluye tildes/ñ) y espacios; no se admite la palabra 'null' ni caracteres especiales, no debe estar vacio")
        String name,

        @NotNull(message = "typeBin no puede ser nulo")
        @Pattern(regexp="DEBITO|CREDITO|PREPAGO",message="typeBin debe ser DEBITO|CREDITO|PREPAGO,no debe estar vacio") String typeBin,

        @NotNull(message = "typeAccount no puede ser nulo")
        @Pattern(regexp="\\d{2}",message="typeAccount debe ser de 2 posiciones numericas,no debe estar vacio") String typeAccount,

        @BlankAsNull
        @SafeText(allowNumbers = true, allowUnderscore = false, allowSpaces = false,
                message = "compensationCod solo permite numeros no se admite la palabra 'null' ni caracteres especiales")
        String compensationCod,

        @BlankAsNull
        @SafeText(allowNumbers = true, allowUnderscore = false, allowSpaces = true,
                message = "description solo permite letras, numeros (incluye tildes/ñ) y espacios; no se admite la palabra 'null' ni caracteres especiales")
        String description,

        @NotNull(message = "usesBinExt no puede ser nulo")
        @Pattern(regexp= "[YN]", message="usesBinExt debe ser 'Y' o 'N', no debe estar vacio") String usesBinExt,
        String createdBy,

        @JsonDeserialize(using = NullOrBlankToNullIntegerDeserializer.class)
        @Min(value = 1, message = "binExtDigits solo permite los numeros 1, 2 o 3 (o la palabra null cuando usesBinExt es igual a N).")
        @Max(value = 3, message = "binExtDigits solo permite los numeros 1, 2 o 3 (o la palabra null cuando usesBinExt es igual a N).")
        Integer binExtDigits
) {}
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto;


import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.jackson.BlankAsNull;
import com.credibanco.authorizer_catalog_bin_manager_cf.shared.validation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ValidationCreateRequest(

        @NotBlank(message = "code no puede ser vacio")
        @NotNull(message = "code no puede ser nulo")
        @SafeText(allowNumbers = true, allowUnderscore = true, allowSpaces = false,
                message = "code solo permite letras, numeros (incluye tildes/ñ) y representar espacios con el caracter _ (underscore) ; no se admite la palabra 'null' ni caracteres especiales") String code,

        @NotNull(message = "description no puede ser nulo")
        @NotBlank(message = "description no puede ser vacio")
        @SafeText(allowNumbers = true, allowUnderscore = false, allowSpaces = true,
                message = "description solo permite letras, numeros (incluye tildes/ñ) y espacios; no se admite la palabra 'null' ni caracteres especiales")
        String description,


        @NotBlank(message = "dataType no puede ser vacio")
        @NotNull(message = "dataType no puede ser nulo")
        @Pattern(regexp = "(?i)^(BOOL|NUMBER|TEXT)$", message = "dataType inválido. Valores permitidos: BOOL | NUMBER | TEXT")
        String dataType,


        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "valueFlag no debe contener caracteres especiales") String valueFlag,
        Double valueNum,
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]*$", message = "valueText no debe contener caracteres especiales")
        String valueText,
        String createdBy
) {}
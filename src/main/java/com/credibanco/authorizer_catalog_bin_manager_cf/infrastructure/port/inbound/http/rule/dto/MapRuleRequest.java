package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.shared.validation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MapRuleRequest(
        @NotNull(message = "subtypeCode no puede ser nulo")
        @NotBlank(message = "subtypeCode no puede ser vacio")
        @Size(min = 3, max = 3,message="subtypeCode debe ser de longitud de 3 posiciones")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "subtypeCode no debe contener caracteres especiales") String subtypeCode,


        @NotNull(message = "bin no puede ser nulo")
        @NotBlank(message = "bin no puede ser vacio")
        @Pattern(regexp="\\d{6,9}", message="bin debe ser numérico de longitud entre 6 y 9 posiciones") String bin,


        @NotBlank(message = "code no puede ser vacio")
        @NotNull(message = "code no puede ser nulo")
        @SafeText(allowNumbers = true, allowUnderscore = true, allowSpaces = false,
                message = "code solo permite letras, numeros (incluye tildes/ñ) y representar espacios con el caracter _ (underscore) ; no se admite la palabra 'null' ni caracteres especiales") String code,

        Object value,
        String updatedBy
) {}
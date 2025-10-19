package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.shared.validation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


public record PlanItemStatus(
        @NotBlank(message = "planCode no puede ser vacio")
        @NotNull(message = "planCode no puede ser nulo")
        @SafeText(allowNumbers = true, allowUnderscore = true, allowSpaces = false,
                message = "planCode solo permite letras, numeros (incluye tildes/ñ) y representar espacios con el caracter _ (underscore) ; no se admite la palabra 'null' ni caracteres especiales") String planCode,

        @NotNull(message = "value no puede ser nulo")
        @Pattern(regexp="\\d*", message="value debe ser numérico")
        @NotBlank(message = "value no puede ser vacio")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\s]+$", message = "value no debe contener caracteres especiales") String value,

        @NotNull(message = "status no puede ser nulo")
        @NotBlank (message = "status no puede ser vacio")
        @NotBlank @Pattern(regexp= "[AI]") String status,

        String updatedBy

) {}
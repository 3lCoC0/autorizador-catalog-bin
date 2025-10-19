package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.shared.validation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PlanUpdateRequest(
        @NotNull(message = "name no puede ser nulo")
        @SafeText(allowNumbers = true, allowUnderscore = false, allowSpaces = true,
                message = "name solo permite letras, numeros (incluye tildes/ñ) y espacios; no se admite la palabra 'null' ni caracteres especiales, no debe estar vacio")
        String name,

        @NotNull(message = "description no puede ser nulo")
        @NotBlank(message = "description no puede ser vacio")
        @SafeText(allowNumbers = true, allowUnderscore = false, allowSpaces = true,
                message = "description solo permite letras, numeros (incluye tildes/ñ) y espacios; no se admite la palabra 'null' ni caracteres especiales")
        String description,

        CommerceValidationMode validationMode,

        String updatedBy
) {}
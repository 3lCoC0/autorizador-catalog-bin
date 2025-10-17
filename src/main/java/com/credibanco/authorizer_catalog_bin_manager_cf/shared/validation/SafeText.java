package com.credibanco.authorizer_catalog_bin_manager_cf.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SafeTextValidator.class)
public @interface SafeText {

    // Mensaje por defecto (puedes mapear a i18n)
    String message() default "solo se permiten letras (incluye tildes/ñ){digits}{underscore}{spaces} y no se admite la palabra 'null'";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /**
     * Permite dígitos 0-9 (Unicode category \\p{N}).
     */
    boolean allowNumbers() default false;

    /**
     * Permite guion bajo '_'.
     */
    boolean allowUnderscore() default false;

    /**
     * Permite espacios (\\s).
     */
    boolean allowSpaces() default true;

    /**
     * Palabra a prohibir como palabra independiente (case-insensitive).
     * Por defecto "null".
     */
    String forbiddenWord() default "null";
}
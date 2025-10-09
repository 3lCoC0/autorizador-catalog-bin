package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = AlphaNumericWithSpacesValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface AlphaNumericWithSpaces {

    String message() default "no debe contener caracteres especiales";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * When {@code true}, blank strings (null or only whitespace) are considered valid.
     */
    boolean allowBlank() default true;
}

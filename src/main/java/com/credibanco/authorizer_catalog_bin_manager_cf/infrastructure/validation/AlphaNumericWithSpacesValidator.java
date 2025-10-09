package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class AlphaNumericWithSpacesValidator implements ConstraintValidator<AlphaNumericWithSpaces, String> {

    private static final Pattern PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\s]+$");

    private boolean allowBlank;

    @Override
    public void initialize(AlphaNumericWithSpaces constraintAnnotation) {
        this.allowBlank = constraintAnnotation.allowBlank();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.isBlank()) {
            return allowBlank;
        }
        return PATTERN.matcher(value).matches();
    }
}

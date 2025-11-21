package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlphaNumericWithSpacesValidatorTest {

    private AlphaNumericWithSpacesValidator validator;

    private AlphaNumericWithSpaces newAnnotation(boolean allowBlank) {
        return new AlphaNumericWithSpaces() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return AlphaNumericWithSpaces.class;
            }

            @Override
            public String message() {
                return "";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return (Class<? extends jakarta.validation.Payload>[]) new Class<?>[0];
            }

            @Override
            public boolean allowBlank() {
                return allowBlank;
            }
        };
    }

    @BeforeEach
    void setup() {
        validator = new AlphaNumericWithSpacesValidator();
    }

    @Test
    void allowsLettersNumbersAndSpaces() {
        validator.initialize(newAnnotation(false));
        assertThat(validator.isValid("Texto 123", null)).isTrue();
        assertThat(validator.isValid("ConAcentos áéíóú", null)).isTrue();
        assertThat(validator.isValid("Con-Guion", null)).isFalse();
    }

    @Test
    void handlesNullAndBlankAccordingToFlag() {
        validator.initialize(newAnnotation(false));
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("   ", null)).isFalse();

        validator.initialize(newAnnotation(true));
        assertThat(validator.isValid("   ", null)).isTrue();
    }
}

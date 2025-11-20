package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationConfigTest {

    private record Sample(@NotNull String value) { }

    @Test
    void createsBeanValidator() {
        ValidationConfig config = new ValidationConfig();
        Validator validator = config.validator();

        assertThat(validator).isNotNull();
        Set<ConstraintViolation<Sample>> violations = validator.validate(new Sample(null));
        assertThat(violations).hasSize(1);
    }
}

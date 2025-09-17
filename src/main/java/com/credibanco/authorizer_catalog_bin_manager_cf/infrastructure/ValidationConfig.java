package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfig {
    @Bean Validator validator() { return Validation.buildDefaultValidatorFactory().getValidator(); }
}

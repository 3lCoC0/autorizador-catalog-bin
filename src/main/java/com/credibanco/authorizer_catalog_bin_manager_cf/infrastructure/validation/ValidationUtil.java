package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
public class ValidationUtil {
    private final Validator validator;
    public ValidationUtil(Validator validator) { this.validator = validator; }

    public <T> Mono<T> validate(T body) {
        var errs = validator.validate(body);
        if (errs.isEmpty()) return Mono.just(body);
        var msg = errs.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("; "));
        return Mono.error(new IllegalArgumentException(msg));
    }
}

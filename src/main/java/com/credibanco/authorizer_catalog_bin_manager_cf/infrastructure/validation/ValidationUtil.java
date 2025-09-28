package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.validation;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
public class ValidationUtil {
    private final Validator validator;
    public ValidationUtil(Validator validator) { this.validator = validator; }

    // Versión tipada por dominio: tú eliges el código
    public <T> Mono<T> validate(T body, AppError invalidCode) {
        var errs = validator.validate(body);
        if (errs.isEmpty()) return Mono.just(body);
        var msg = errs.stream().map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return Mono.error(new AppException(invalidCode, msg));
    }
}

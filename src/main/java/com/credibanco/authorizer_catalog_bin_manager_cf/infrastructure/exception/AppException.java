package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception;


import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final AppError error;

    public AppException(AppError error) {
        super(error.defaultMessage);
        this.error = error;
    }

    public AppException(AppError error, String overrideMessage) {
        super(overrideMessage == null ? error.defaultMessage : overrideMessage);
        this.error = error;
    }

}

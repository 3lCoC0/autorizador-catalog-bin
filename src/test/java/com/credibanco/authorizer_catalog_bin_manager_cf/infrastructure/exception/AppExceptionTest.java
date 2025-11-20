package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppExceptionTest {

    @Test
    void usesDefaultMessageWhenOverrideIsNull() {
        AppException ex = new AppException(AppError.AGENCY_NOT_FOUND);
        assertThat(ex.getError()).isEqualTo(AppError.AGENCY_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo(AppError.AGENCY_NOT_FOUND.defaultMessage);
    }

    @Test
    void appliesOverrideMessage() {
        AppException ex = new AppException(AppError.AGENCY_NOT_FOUND, "custom");
        assertThat(ex.getMessage()).isEqualTo("custom");
    }
}

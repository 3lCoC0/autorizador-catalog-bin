package com.credibanco.authorizer_catalog_bin_manager_cf;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorizerCatalogBinManagerCfApplicationTest {

    private TimeZone originalZone;

    @BeforeEach
    void setUp() {
        originalZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalZone);
    }

    @Test
    void mainSetsDefaultTimezoneAndRunsSpringApplication() {
        String[] args = new String[] {"--test"};

        try (MockedStatic<SpringApplication> spring = Mockito.mockStatic(SpringApplication.class)) {
            AuthorizerCatalogBinManagerCfApplication.main(args);

            spring.verify(() -> SpringApplication.run(AuthorizerCatalogBinManagerCfApplication.class, args));
            assertEquals("America/Bogota", TimeZone.getDefault().getID());
        }
    }
}

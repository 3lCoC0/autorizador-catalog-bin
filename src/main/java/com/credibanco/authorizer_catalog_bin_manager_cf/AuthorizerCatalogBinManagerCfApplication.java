package com.credibanco.authorizer_catalog_bin_manager_cf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class AuthorizerCatalogBinManagerCfApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
		SpringApplication.run(AuthorizerCatalogBinManagerCfApplication.class, args);
	}

}

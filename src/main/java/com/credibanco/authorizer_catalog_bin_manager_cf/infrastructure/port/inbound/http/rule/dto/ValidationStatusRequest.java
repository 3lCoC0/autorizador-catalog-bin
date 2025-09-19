package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.rule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record ValidationStatusRequest(
        @NotBlank @Pattern(regexp="A|I") String status
) {}

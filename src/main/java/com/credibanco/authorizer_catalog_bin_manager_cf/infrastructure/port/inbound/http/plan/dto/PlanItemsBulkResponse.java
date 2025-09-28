package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import java.util.List;

public record PlanItemsBulkResponse(
        String planCode,
        int requested,
        int inserted,
        int duplicates,
        int invalid,
        List<String> invalidValues,
        List<String> duplicateValues
) {}

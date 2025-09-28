package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.model;

import java.util.List;

public record PlanItemsBulkResult(
        String planCode,
        int requested,
        int inserted,
        int duplicates,
        int invalid,
        List<String> invalidValues,
        List<String> duplicateValues
) {}

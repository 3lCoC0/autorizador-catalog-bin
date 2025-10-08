package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.inbound.http.plan.dto;

import java.util.List;

public record PlanItemsListResponse(
        PlanResponse plan,
        int page,
        int size,
        String statusFilter,
        int count,
        List<PlanItemResponse> items
) {}

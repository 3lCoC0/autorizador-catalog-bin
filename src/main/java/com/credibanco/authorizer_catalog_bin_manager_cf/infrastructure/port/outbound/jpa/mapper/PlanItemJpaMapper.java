package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.CommercePlanItemEntity;

public final class PlanItemJpaMapper {

    private PlanItemJpaMapper() {
    }

    public static PlanItem toDomain(CommercePlanItemEntity entity) {
        return PlanItem.rehydrate(
                entity.getPlanItemId(),
                entity.getPlanId(),
                entity.getValue(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy(),
                entity.getStatus()
        );
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeCommercePlanEntity;

public final class SubtypePlanJpaMapper {

    private SubtypePlanJpaMapper() {
    }

    public static SubtypeCommercePlanEntity toEntity(SubtypePlanLink link) {
        SubtypeCommercePlanEntity entity = new SubtypeCommercePlanEntity();
        entity.setSubtypePlanId(null);
        entity.setSubtypeCode(link.subtypeCode());
        entity.setPlanId(link.planId());
        entity.setCreatedAt(link.createdAt());
        entity.setUpdatedAt(link.updatedAt());
        entity.setUpdatedBy(link.updatedBy());
        return entity;
    }

    public static SubtypePlanLink toDomain(SubtypeCommercePlanEntity entity) {
        return SubtypePlanLink.rehydrate(
                entity.getSubtypeCode(),
                entity.getPlanId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.CommercePlanEntity;

public final class CommercePlanJpaMapper {

    private CommercePlanJpaMapper() {
    }

    public static CommercePlanEntity toEntity(CommercePlan domain) {
        CommercePlanEntity entity = new CommercePlanEntity();
        entity.setPlanId(domain.planId());
        entity.setPlanCode(domain.code());
        entity.setPlanName(domain.name());
        entity.setValidationMode(domain.validationMode().name());
        entity.setDescription(domain.description());
        entity.setStatus(domain.status());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        entity.setUpdatedBy(domain.updatedBy());
        return entity;
    }

    public static CommercePlan toDomain(CommercePlanEntity entity) {
        return CommercePlan.rehydrate(
                entity.getPlanId(),
                entity.getPlanCode(),
                entity.getPlanName(),
                CommerceValidationMode.valueOf(entity.getValidationMode()),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }
}

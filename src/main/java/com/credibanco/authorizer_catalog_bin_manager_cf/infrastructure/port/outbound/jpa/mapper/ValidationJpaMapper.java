package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.ValidationEntity;

public final class ValidationJpaMapper {

    private ValidationJpaMapper() {
    }

    public static ValidationEntity toEntity(Validation domain) {
        ValidationEntity entity = new ValidationEntity();
        entity.setValidationId(domain.validationId());
        entity.setCode(domain.code());
        entity.setDescription(domain.description());
        entity.setDataType(domain.dataType().name());
        entity.setStatus(domain.status());
        entity.setValidFrom(domain.validFrom());
        entity.setValidTo(domain.validTo());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        entity.setUpdatedBy(domain.updatedBy());
        return entity;
    }

    public static Validation toDomain(ValidationEntity entity) {
        return Validation.rehydrate(
                entity.getValidationId(),
                entity.getCode(),
                entity.getDescription(),
                ValidationDataType.valueOf(entity.getDataType()),
                entity.getStatus(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }
}

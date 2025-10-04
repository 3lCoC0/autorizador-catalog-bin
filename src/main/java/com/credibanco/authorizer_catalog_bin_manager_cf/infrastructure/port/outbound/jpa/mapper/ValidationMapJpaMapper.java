package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.ValidationMapEntity;

public final class ValidationMapJpaMapper {

    private ValidationMapJpaMapper() {
    }

    public static ValidationMapEntity toEntity(ValidationMap map) {
        ValidationMapEntity entity = new ValidationMapEntity();
        entity.setMapId(map.mapId());
        entity.setSubtypeCode(map.subtypeCode());
        entity.setBin(map.bin());
        entity.setValidationId(map.validationId());
        entity.setStatus(map.status());
        entity.setValueFlag(map.valueFlag());
        entity.setValueNum(map.valueNum());
        entity.setValueText(map.valueText());
        entity.setCreatedAt(map.createdAt());
        entity.setUpdatedAt(map.updatedAt());
        entity.setUpdatedBy(map.updatedBy());
        return entity;
    }

    public static ValidationMap toDomain(ValidationMapEntity entity) {
        return ValidationMap.rehydrate(
                entity.getMapId(),
                entity.getSubtypeCode(),
                entity.getBin(),
                entity.getValidationId(),
                entity.getStatus(),
                entity.getValueFlag(),
                entity.getValueNum(),
                entity.getValueText(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }
}

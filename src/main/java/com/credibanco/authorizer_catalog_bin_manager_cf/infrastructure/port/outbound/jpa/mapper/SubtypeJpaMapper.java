package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeEntityId;

public final class SubtypeJpaMapper {

    private SubtypeJpaMapper() {
    }

    public static SubtypeEntity toEntity(Subtype domain) {
        SubtypeEntity entity = new SubtypeEntity();
        entity.setId(new SubtypeEntityId(domain.subtypeCode(), domain.bin()));
        entity.setName(domain.name());
        entity.setDescription(domain.description());
        entity.setStatus(domain.status());
        entity.setOwnerIdType(domain.ownerIdType());
        entity.setOwnerIdNumber(domain.ownerIdNumber());
        entity.setBinExt(domain.binExt());
        entity.setBinEfectivo(domain.binEfectivo());
        entity.setSubtypeId(domain.subtypeId());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        entity.setUpdatedBy(domain.updatedBy());
        return entity;
    }

    public static Subtype toDomain(SubtypeEntity entity) {
        return Subtype.rehydrate(
                entity.getId().getSubtypeCode(),
                entity.getId().getBin(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getOwnerIdType(),
                entity.getOwnerIdNumber(),
                entity.getBinExt(),
                entity.getBinEfectivo(),
                entity.getSubtypeId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.BinEntity;

public final class BinJpaMapper {

    private BinJpaMapper() {
    }

    public static BinEntity toEntity(Bin domain) {
        BinEntity entity = new BinEntity();
        entity.setBin(domain.bin());
        entity.setName(domain.name());
        entity.setTypeBin(domain.typeBin());
        entity.setTypeAccount(domain.typeAccount());
        entity.setCompensationCod(domain.compensationCod());
        entity.setDescription(domain.description());
        entity.setStatus(domain.status());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        entity.setUpdatedBy(domain.updatedBy());
        entity.setUsesBinExt(domain.usesBinExt());
        entity.setBinExtDigits(domain.binExtDigits());
        return entity;
    }

    public static Bin toDomain(BinEntity entity) {
        return Bin.rehydrate(
                entity.getBin(),
                entity.getName(),
                entity.getTypeBin(),
                entity.getTypeAccount(),
                entity.getCompensationCod(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy(),
                entity.getUsesBinExt(),
                entity.getBinExtDigits()
        );
    }
}

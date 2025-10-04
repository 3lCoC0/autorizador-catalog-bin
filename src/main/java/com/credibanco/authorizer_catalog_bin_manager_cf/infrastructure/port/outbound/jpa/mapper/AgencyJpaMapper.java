package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.AgencyEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.AgencyEntityId;

public final class AgencyJpaMapper {

    private AgencyJpaMapper() {
    }

    public static AgencyEntity toEntity(Agency domain) {
        AgencyEntity entity = new AgencyEntity();
        entity.setId(new AgencyEntityId(domain.subtypeCode(), domain.agencyCode()));
        entity.setName(domain.name());
        entity.setAgencyNit(domain.agencyNit());
        entity.setAddress(domain.address());
        entity.setPhone(domain.phone());
        entity.setMunicipalityDaneCode(domain.municipalityDaneCode());
        entity.setEmbosserHighlight(domain.embosserHighlight());
        entity.setEmbosserPins(domain.embosserPins());
        entity.setCardCustodianPrimary(domain.cardCustodianPrimary());
        entity.setCardCustodianPrimaryId(domain.cardCustodianPrimaryId());
        entity.setCardCustodianSecondary(domain.cardCustodianSecondary());
        entity.setCardCustodianSecondaryId(domain.cardCustodianSecondaryId());
        entity.setPinCustodianPrimary(domain.pinCustodianPrimary());
        entity.setPinCustodianPrimaryId(domain.pinCustodianPrimaryId());
        entity.setPinCustodianSecondary(domain.pinCustodianSecondary());
        entity.setPinCustodianSecondaryId(domain.pinCustodianSecondaryId());
        entity.setDescription(domain.description());
        entity.setStatus(domain.status());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        entity.setUpdatedBy(domain.updatedBy());
        return entity;
    }

    public static Agency toDomain(AgencyEntity entity) {
        return Agency.rehydrate(
                entity.getId().getSubtypeCode(),
                entity.getId().getAgencyCode(),
                entity.getName(),
                entity.getAgencyNit(),
                entity.getAddress(),
                entity.getPhone(),
                entity.getMunicipalityDaneCode(),
                entity.getEmbosserHighlight(),
                entity.getEmbosserPins(),
                entity.getCardCustodianPrimary(),
                entity.getCardCustodianPrimaryId(),
                entity.getCardCustodianSecondary(),
                entity.getCardCustodianSecondaryId(),
                entity.getPinCustodianPrimary(),
                entity.getPinCustodianPrimaryId(),
                entity.getPinCustodianSecondary(),
                entity.getPinCustodianSecondaryId(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }
}

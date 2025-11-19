package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.AgencyEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.AgencyEntityId;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.AgencyJpaMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AgencyJpaMapperTest {

    @Test
    void toEntityCopiesAllFields() {
        OffsetDateTime created = OffsetDateTime.now().minusDays(1);
        OffsetDateTime updated = created.plusHours(2);
        Agency domain = Agency.rehydrate(
                "SUB", "01", "Main", "123", "Street", "555", "05001",
                "HL", "1234",
                "JOHN", "123", "JANE", "234",
                "MARY", "345", "MIKE", "456",
                "Desc", "A", created, updated, "actor"
        );

        AgencyEntity entity = AgencyJpaMapper.toEntity(domain);

        assertEquals("SUB", entity.getId().getSubtypeCode());
        assertEquals("01", entity.getId().getAgencyCode());
        assertEquals("Main", entity.getName());
        assertEquals("Desc", entity.getDescription());
        assertEquals("A", entity.getStatus());
        assertEquals(created, entity.getCreatedAt());
        assertEquals(updated, entity.getUpdatedAt());
        assertEquals("actor", entity.getUpdatedBy());
    }

    @Test
    void toDomainRehydratesAggregate() {
        OffsetDateTime created = OffsetDateTime.now().minusDays(1);
        OffsetDateTime updated = created.plusHours(2);
        AgencyEntity entity = new AgencyEntity();
        entity.setId(new AgencyEntityId("SUB", "01"));
        entity.setName("Main");
        entity.setAgencyNit("123");
        entity.setAddress("Street");
        entity.setPhone("555");
        entity.setMunicipalityDaneCode("05001");
        entity.setEmbosserHighlight("HL");
        entity.setEmbosserPins("1234");
        entity.setCardCustodianPrimary("JOHN");
        entity.setCardCustodianPrimaryId("123");
        entity.setCardCustodianSecondary("JANE");
        entity.setCardCustodianSecondaryId("234");
        entity.setPinCustodianPrimary("MARY");
        entity.setPinCustodianPrimaryId("345");
        entity.setPinCustodianSecondary("MIKE");
        entity.setPinCustodianSecondaryId("456");
        entity.setDescription("Desc");
        entity.setStatus("I");
        entity.setCreatedAt(created);
        entity.setUpdatedAt(updated);
        entity.setUpdatedBy("actor");

        Agency domain = AgencyJpaMapper.toDomain(entity);

        assertEquals("SUB", domain.subtypeCode());
        assertEquals("01", domain.agencyCode());
        assertEquals("Main", domain.name());
        assertEquals("I", domain.status());
        assertEquals(created, domain.createdAt());
        assertEquals(updated, domain.updatedAt());
        assertEquals("actor", domain.updatedBy());
    }
}

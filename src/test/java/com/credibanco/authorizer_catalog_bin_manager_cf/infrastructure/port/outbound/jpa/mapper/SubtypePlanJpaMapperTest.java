package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeCommercePlanEntity;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SubtypePlanJpaMapperTest {

    @Test
    void mapsDomainToEntity() {
        OffsetDateTime created = OffsetDateTime.now().minusDays(1);
        OffsetDateTime updated = OffsetDateTime.now();
        SubtypePlanLink link = new SubtypePlanLink("ST", 2L, created, updated, "tester");

        SubtypeCommercePlanEntity entity = SubtypePlanJpaMapper.toEntity(link);

        assertNull(entity.getSubtypePlanId());
        assertEquals("ST", entity.getSubtypeCode());
        assertEquals(2L, entity.getPlanId());
        assertEquals(created, entity.getCreatedAt());
        assertEquals(updated, entity.getUpdatedAt());
        assertEquals("tester", entity.getUpdatedBy());
    }

    @Test
    void mapsEntityToDomain() {
        OffsetDateTime created = OffsetDateTime.now().minusDays(3);
        OffsetDateTime updated = OffsetDateTime.now().minusDays(1);
        SubtypeCommercePlanEntity entity = new SubtypeCommercePlanEntity();
        entity.setSubtypePlanId(8L);
        entity.setSubtypeCode("ST2");
        entity.setPlanId(5L);
        entity.setCreatedAt(created);
        entity.setUpdatedAt(updated);
        entity.setUpdatedBy("actor");

        SubtypePlanLink link = SubtypePlanJpaMapper.toDomain(entity);

        assertEquals("ST2", link.subtypeCode());
        assertEquals(5L, link.planId());
        assertEquals(created, link.createdAt());
        assertEquals(updated, link.updatedAt());
        assertEquals("actor", link.updatedBy());
    }
}

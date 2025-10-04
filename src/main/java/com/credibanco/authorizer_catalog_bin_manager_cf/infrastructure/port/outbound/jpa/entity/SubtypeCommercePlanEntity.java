package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "SUBTYPE_COMMERCE_PLAN")
public class SubtypeCommercePlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subtypePlanSeq")
    @SequenceGenerator(name = "subtypePlanSeq", sequenceName = "SEQ_SUBTYPE_COMMERCE_PLAN_ID", allocationSize = 1)
    @Column(name = "SUBTYPE_PLAN_ID")
    private Long subtypePlanId;

    @Column(name = "SUBTYPE_CODE", nullable = false, unique = true)
    private String subtypeCode;

    @Column(name = "PLAN_ID", nullable = false)
    private Long planId;

    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    public Long getSubtypePlanId() {
        return subtypePlanId;
    }

    public void setSubtypePlanId(Long subtypePlanId) {
        this.subtypePlanId = subtypePlanId;
    }

    public String getSubtypeCode() {
        return subtypeCode;
    }

    public void setSubtypeCode(String subtypeCode) {
        this.subtypeCode = subtypeCode;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}

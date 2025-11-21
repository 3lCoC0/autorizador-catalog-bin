package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name = "COMMERCE_PLAN")
public class CommercePlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "commercePlanSeq")
    @SequenceGenerator(name = "commercePlanSeq", sequenceName = "SEQ_COMMERCE_PLAN_ID", allocationSize = 1)
    @Column(name = "PLAN_ID")
    private Long planId;

    @Column(name = "PLAN_CODE", nullable = false, unique = true)
    private String planCode;

    @Column(name = "PLAN_NAME", nullable = false)
    private String planName;

    @Column(name = "VALIDATION_MODE", nullable = false)
    private String validationMode;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "STATUS", nullable = false, length = 1)
    private String status;

    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

}

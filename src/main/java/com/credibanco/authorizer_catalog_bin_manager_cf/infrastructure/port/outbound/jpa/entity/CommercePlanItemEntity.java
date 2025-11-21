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
@Table(name = "COMMERCE_PLAN_ITEM")
public class CommercePlanItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "commercePlanItemSeq")
    @SequenceGenerator(name = "commercePlanItemSeq", sequenceName = "SEQ_COMMERCE_PLAN_ITEM_ID", allocationSize = 1)
    @Column(name = "PLAN_ITEM_ID")
    private Long planItemId;

    @Column(name = "PLAN_ID", nullable = false)
    private Long planId;

    @Column(name = "MCC")
    private String mcc;

    @Column(name = "MERCHANT_ID")
    private String merchantId;

    @Column(name = "STATUS", nullable = false, length = 1)
    private String status;

    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    public String getValue() {
        return mcc != null ? mcc : merchantId;
    }
}

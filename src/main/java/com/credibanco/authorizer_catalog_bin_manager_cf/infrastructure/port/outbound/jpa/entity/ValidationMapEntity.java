package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "SUBTYPE_VALIDATION_MAP")
public class ValidationMapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "validationMapSeq")
    @SequenceGenerator(name = "validationMapSeq", sequenceName = "SEQ_SUBTYPE_VAL_MAP_ID", allocationSize = 1)
    @Column(name = "SUBTYPE_VAL_MAP_ID")
    private Long mapId;

    @Column(name = "SUBTYPE_CODE", nullable = false)
    private String subtypeCode;

    @Column(name = "BIN", nullable = false)
    private String bin;

    @Column(name = "VALIDATION_ID", nullable = false)
    private Long validationId;

    @Column(name = "STATUS", nullable = false, length = 1)
    private String status;

    @Column(name = "VALUE_FLAG")
    private String valueFlag;

    @Column(name = "VALUE_NUM")
    private Double valueNum;

    @Column(name = "VALUE_TEXT")
    private String valueText;

    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VALIDATION_ID", referencedColumnName = "VALIDATION_ID", insertable = false, updatable = false)
    private ValidationEntity validation;

    public Long getMapId() {
        return mapId;
    }

    public void setMapId(Long mapId) {
        this.mapId = mapId;
    }

    public String getSubtypeCode() {
        return subtypeCode;
    }

    public void setSubtypeCode(String subtypeCode) {
        this.subtypeCode = subtypeCode;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public Long getValidationId() {
        return validationId;
    }

    public void setValidationId(Long validationId) {
        this.validationId = validationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getValueFlag() {
        return valueFlag;
    }

    public void setValueFlag(String valueFlag) {
        this.valueFlag = valueFlag;
    }

    public Double getValueNum() {
        return valueNum;
    }

    public void setValueNum(Double valueNum) {
        this.valueNum = valueNum;
    }

    public String getValueText() {
        return valueText;
    }

    public void setValueText(String valueText) {
        this.valueText = valueText;
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

    public ValidationEntity getValidation() {
        return validation;
    }

    public void setValidation(ValidationEntity validation) {
        this.validation = validation;
    }
}

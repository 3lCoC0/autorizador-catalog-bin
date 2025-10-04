package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "BIN")
public class BinEntity {

    @Id
    @Column(name = "BIN", nullable = false, length = 9)
    private String bin;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "TYPE_BIN", nullable = false)
    private String typeBin;

    @Column(name = "TYPE_ACCOUNT", nullable = false)
    private String typeAccount;

    @Column(name = "COMPENSATION_COD")
    private String compensationCod;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "STATUS", nullable = false, length = 1)
    private String status;

    @Column(name = "CREATED_AT", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    @Column(name = "USES_BIN_EXT", nullable = false, length = 1)
    private String usesBinExt;

    @Column(name = "BIN_EXT_DIGITS")
    private Integer binExtDigits;

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeBin() {
        return typeBin;
    }

    public void setTypeBin(String typeBin) {
        this.typeBin = typeBin;
    }

    public String getTypeAccount() {
        return typeAccount;
    }

    public void setTypeAccount(String typeAccount) {
        this.typeAccount = typeAccount;
    }

    public String getCompensationCod() {
        return compensationCod;
    }

    public void setCompensationCod(String compensationCod) {
        this.compensationCod = compensationCod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getUsesBinExt() {
        return usesBinExt;
    }

    public void setUsesBinExt(String usesBinExt) {
        this.usesBinExt = usesBinExt;
    }

    public Integer getBinExtDigits() {
        return binExtDigits;
    }

    public void setBinExtDigits(Integer binExtDigits) {
        this.binExtDigits = binExtDigits;
    }
}

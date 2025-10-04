package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "SUBTYPE")
public class SubtypeEntity {

    @EmbeddedId
    private SubtypeEntityId id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "STATUS", nullable = false, length = 1)
    private String status;

    @Column(name = "OWNER_ID_TYPE")
    private String ownerIdType;

    @Column(name = "OWNER_ID_NUMBER")
    private String ownerIdNumber;

    @Column(name = "BIN_EXT")
    private String binExt;

    @Column(name = "BIN_EFECTIVO", nullable = false)
    private String binEfectivo;

    @Column(name = "SUBTYPE_ID", insertable = false, updatable = false)
    private Long subtypeId;

    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    public SubtypeEntityId getId() {
        return id;
    }

    public void setId(SubtypeEntityId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getOwnerIdType() {
        return ownerIdType;
    }

    public void setOwnerIdType(String ownerIdType) {
        this.ownerIdType = ownerIdType;
    }

    public String getOwnerIdNumber() {
        return ownerIdNumber;
    }

    public void setOwnerIdNumber(String ownerIdNumber) {
        this.ownerIdNumber = ownerIdNumber;
    }

    public String getBinExt() {
        return binExt;
    }

    public void setBinExt(String binExt) {
        this.binExt = binExt;
    }

    public String getBinEfectivo() {
        return binEfectivo;
    }

    public void setBinEfectivo(String binEfectivo) {
        this.binEfectivo = binEfectivo;
    }

    public Long getSubtypeId() {
        return subtypeId;
    }

    public void setSubtypeId(Long subtypeId) {
        this.subtypeId = subtypeId;
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

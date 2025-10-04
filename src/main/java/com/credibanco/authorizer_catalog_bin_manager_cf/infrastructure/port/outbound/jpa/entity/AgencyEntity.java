package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "AGENCY")
public class AgencyEntity {

    @EmbeddedId
    private AgencyEntityId id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "AGENCY_NIT")
    private String agencyNit;

    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "PHONE")
    private String phone;

    @Column(name = "MUNICIPALITY_DANE_CODE")
    private String municipalityDaneCode;

    @Column(name = "EMBOSSER_HIGHLIGHT")
    private String embosserHighlight;

    @Column(name = "EMBOSSER_PINS")
    private String embosserPins;

    @Column(name = "CARD_CUSTODIAN_PRIMARY")
    private String cardCustodianPrimary;

    @Column(name = "CARD_CUSTODIAN_PRIMARY_ID")
    private String cardCustodianPrimaryId;

    @Column(name = "CARD_CUSTODIAN_SECONDARY")
    private String cardCustodianSecondary;

    @Column(name = "CARD_CUSTODIAN_SECONDARY_ID")
    private String cardCustodianSecondaryId;

    @Column(name = "PIN_CUSTODIAN_PRIMARY")
    private String pinCustodianPrimary;

    @Column(name = "PIN_CUSTODIAN_PRIMARY_ID")
    private String pinCustodianPrimaryId;

    @Column(name = "PIN_CUSTODIAN_SECONDARY")
    private String pinCustodianSecondary;

    @Column(name = "PIN_CUSTODIAN_SECONDARY_ID")
    private String pinCustodianSecondaryId;

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

    public AgencyEntityId getId() {
        return id;
    }

    public void setId(AgencyEntityId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAgencyNit() {
        return agencyNit;
    }

    public void setAgencyNit(String agencyNit) {
        this.agencyNit = agencyNit;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMunicipalityDaneCode() {
        return municipalityDaneCode;
    }

    public void setMunicipalityDaneCode(String municipalityDaneCode) {
        this.municipalityDaneCode = municipalityDaneCode;
    }

    public String getEmbosserHighlight() {
        return embosserHighlight;
    }

    public void setEmbosserHighlight(String embosserHighlight) {
        this.embosserHighlight = embosserHighlight;
    }

    public String getEmbosserPins() {
        return embosserPins;
    }

    public void setEmbosserPins(String embosserPins) {
        this.embosserPins = embosserPins;
    }

    public String getCardCustodianPrimary() {
        return cardCustodianPrimary;
    }

    public void setCardCustodianPrimary(String cardCustodianPrimary) {
        this.cardCustodianPrimary = cardCustodianPrimary;
    }

    public String getCardCustodianPrimaryId() {
        return cardCustodianPrimaryId;
    }

    public void setCardCustodianPrimaryId(String cardCustodianPrimaryId) {
        this.cardCustodianPrimaryId = cardCustodianPrimaryId;
    }

    public String getCardCustodianSecondary() {
        return cardCustodianSecondary;
    }

    public void setCardCustodianSecondary(String cardCustodianSecondary) {
        this.cardCustodianSecondary = cardCustodianSecondary;
    }

    public String getCardCustodianSecondaryId() {
        return cardCustodianSecondaryId;
    }

    public void setCardCustodianSecondaryId(String cardCustodianSecondaryId) {
        this.cardCustodianSecondaryId = cardCustodianSecondaryId;
    }

    public String getPinCustodianPrimary() {
        return pinCustodianPrimary;
    }

    public void setPinCustodianPrimary(String pinCustodianPrimary) {
        this.pinCustodianPrimary = pinCustodianPrimary;
    }

    public String getPinCustodianPrimaryId() {
        return pinCustodianPrimaryId;
    }

    public void setPinCustodianPrimaryId(String pinCustodianPrimaryId) {
        this.pinCustodianPrimaryId = pinCustodianPrimaryId;
    }

    public String getPinCustodianSecondary() {
        return pinCustodianSecondary;
    }

    public void setPinCustodianSecondary(String pinCustodianSecondary) {
        this.pinCustodianSecondary = pinCustodianSecondary;
    }

    public String getPinCustodianSecondaryId() {
        return pinCustodianSecondaryId;
    }

    public void setPinCustodianSecondaryId(String pinCustodianSecondaryId) {
        this.pinCustodianSecondaryId = pinCustodianSecondaryId;
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
}

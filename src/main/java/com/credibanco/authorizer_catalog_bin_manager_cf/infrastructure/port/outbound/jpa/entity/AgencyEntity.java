package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
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

}

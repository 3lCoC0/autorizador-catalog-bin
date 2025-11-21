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
@Table(name = "SUBTYPE_VALIDATION")
public class ValidationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "validationSeq")
    @SequenceGenerator(name = "validationSeq", sequenceName = "SEQ_SUBTIPO_VALIDATION_ID", allocationSize = 1)
    @Column(name = "VALIDATION_ID")
    private Long validationId;

    @Column(name = "CODE", nullable = false, unique = true)
    private String code;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DATA_TYPE", nullable = false)
    private String dataType;

    @Column(name = "STATUS", nullable = false, length = 1)
    private String status;

    @Column(name = "VALID_FROM")
    private OffsetDateTime validFrom;

    @Column(name = "VALID_TO")
    private OffsetDateTime validTo;

    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

}

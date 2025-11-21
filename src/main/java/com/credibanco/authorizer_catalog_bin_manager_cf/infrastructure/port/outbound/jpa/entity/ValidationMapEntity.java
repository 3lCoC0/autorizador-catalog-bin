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
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
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

}

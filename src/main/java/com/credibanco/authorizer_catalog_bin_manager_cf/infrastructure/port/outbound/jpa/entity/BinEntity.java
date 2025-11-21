package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
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

}

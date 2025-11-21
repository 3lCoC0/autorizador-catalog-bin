package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "ID_TYPE")
public class IdTypeEntity {

    @Id
    @Column(name = "ID_TYPE_CODE", nullable = false)
    private String idTypeCode;

}

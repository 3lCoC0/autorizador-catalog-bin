package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@Embeddable
public class AgencyEntityId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "SUBTYPE_CODE", nullable = false, length = 3)
    private String subtypeCode;

    @Column(name = "AGENCY_CODE", nullable = false)
    private String agencyCode;


    public AgencyEntityId(String subtypeCode, String agencyCode) {
        this.subtypeCode = subtypeCode;
        this.agencyCode = agencyCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgencyEntityId that = (AgencyEntityId) o;
        return Objects.equals(subtypeCode, that.subtypeCode) && Objects.equals(agencyCode, that.agencyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subtypeCode, agencyCode);
    }
}

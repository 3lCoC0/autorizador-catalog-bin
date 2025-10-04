package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AgencyEntityId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "SUBTYPE_CODE", nullable = false, length = 3)
    private String subtypeCode;

    @Column(name = "AGENCY_CODE", nullable = false)
    private String agencyCode;

    public AgencyEntityId() {
    }

    public AgencyEntityId(String subtypeCode, String agencyCode) {
        this.subtypeCode = subtypeCode;
        this.agencyCode = agencyCode;
    }

    public String getSubtypeCode() {
        return subtypeCode;
    }

    public void setSubtypeCode(String subtypeCode) {
        this.subtypeCode = subtypeCode;
    }

    public String getAgencyCode() {
        return agencyCode;
    }

    public void setAgencyCode(String agencyCode) {
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

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SubtypeEntityId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "SUBTYPE_CODE", nullable = false, length = 3)
    private String subtypeCode;

    @Column(name = "BIN", nullable = false, length = 9)
    private String bin;

    public SubtypeEntityId() {
    }

    public SubtypeEntityId(String subtypeCode, String bin) {
        this.subtypeCode = subtypeCode;
        this.bin = bin;
    }

    public String getSubtypeCode() {
        return subtypeCode;
    }

    public void setSubtypeCode(String subtypeCode) {
        this.subtypeCode = subtypeCode;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubtypeEntityId that = (SubtypeEntityId) o;
        return Objects.equals(subtypeCode, that.subtypeCode) && Objects.equals(bin, that.bin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subtypeCode, bin);
    }
}

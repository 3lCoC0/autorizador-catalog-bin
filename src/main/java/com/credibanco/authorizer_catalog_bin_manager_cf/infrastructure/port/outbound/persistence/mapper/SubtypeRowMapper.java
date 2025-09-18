package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.persistence.mapper;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import io.r2dbc.spi.Row;

import java.time.LocalDateTime;



final class SubtypeRowMapper {
    static Subtype map(Row r) {
        return Subtype.rehydrate(
                r.get("SUBTYPE_CODE", String.class),
                r.get("BIN",           String.class),
                r.get("NAME",          String.class),
                r.get("DESCRIPTION",   String.class),
                r.get("STATUS",        String.class),
                r.get("OWNER_ID_TYPE", String.class),
                r.get("OWNER_ID_NUMBER", String.class),
                r.get("BIN_EXT",       String.class),
                r.get("BIN_EFECTIVO",  String.class),
                r.get("SUBTYPE_ID",    Long.class),
                toOffset(r, "CREATED_AT"),
                r.get("CREATED_AT",    LocalDateTime.class),
                r.get("UPDATED_AT",    LocalDateTime.class),
                r.get("UPDATED_BY",    String.class)
        );
    }
    private SubtypeRowMapper() {}
}
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.persistence;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.function.BiFunction;

@Repository
public class R2dbcAgencyRepository implements AgencyRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final DatabaseClient db;

    public R2dbcAgencyRepository(DatabaseClient db) { this.db = db; }

    private static OffsetDateTime toOffset(Row row, String col) {
        LocalDateTime ldt = row.get(col, LocalDateTime.class);
        if (ldt != null) return ldt.atZone(ZONE).toOffsetDateTime();
        return row.get(col, OffsetDateTime.class);
    }

    private static final BiFunction<Row, RowMetadata, Agency> MAPPER = (r, m) ->
            Agency.rehydrate(
                    r.get("subtype_code", String.class),
                    r.get("agency_code", String.class),
                    r.get("name", String.class),
                    r.get("agency_nit", String.class),
                    r.get("address", String.class),
                    r.get("phone", String.class),
                    r.get("municipality_dane_code", String.class),
                    r.get("embosser_highlight", String.class),
                    r.get("embosser_pins", String.class),
                    r.get("card_custodian_primary", String.class),
                    r.get("card_custodian_primary_id", String.class),
                    r.get("card_custodian_secondary", String.class),
                    r.get("card_custodian_secondary_id", String.class),
                    r.get("pin_custodian_primary", String.class),
                    r.get("pin_custodian_primary_id", String.class),
                    r.get("pin_custodian_secondary", String.class),
                    r.get("pin_custodian_secondary_id", String.class),
                    r.get("description", String.class),
                    r.get("status", String.class),
                    toOffset(r, "created_at"),
                    toOffset(r, "updated_at"),
                    r.get("updated_by", String.class)
            );

    @Override
    public Mono<Boolean> existsByPk(String subtypeCode, String agencyCode) {
        return db.sql("""
                SELECT 1 FROM AGENCY
                 WHERE SUBTYPE_CODE=:st AND AGENCY_CODE=:ag
                 FETCH FIRST 1 ROWS ONLY
                """)
                .bind("st", subtypeCode)
                .bind("ag", agencyCode)
                .map((r,m) -> 1)
                .first()
                .map(x -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Agency> findByPk(String subtypeCode, String agencyCode) {
        return db.sql("""
                SELECT SUBTYPE_CODE, AGENCY_CODE, NAME, AGENCY_NIT, ADDRESS, PHONE,
                       MUNICIPALITY_DANE_CODE, EMBOSSER_HIGHLIGHT, EMBOSSER_PINS,
                       CARD_CUSTODIAN_PRIMARY, CARD_CUSTODIAN_PRIMARY_ID,
                       CARD_CUSTODIAN_SECONDARY, CARD_CUSTODIAN_SECONDARY_ID,
                       PIN_CUSTODIAN_PRIMARY, PIN_CUSTODIAN_PRIMARY_ID,
                       PIN_CUSTODIAN_SECONDARY, PIN_CUSTODIAN_SECONDARY_ID,
                       DESCRIPTION, STATUS, CREATED_AT, UPDATED_AT, UPDATED_BY
                  FROM AGENCY
                 WHERE SUBTYPE_CODE=:st AND AGENCY_CODE=:ag
                """)
                .bind("st", subtypeCode)
                .bind("ag", agencyCode)
                .map(MAPPER)
                .one();
    }

    @Override
    public Flux<Agency> findAll(String subtypeCode, String status, String search, int page, int size) {
        int p = Math.max(0, page);
        int s = Math.max(1, size);
        int offset = p * s;

        StringBuilder sql = new StringBuilder("""
                SELECT SUBTYPE_CODE, AGENCY_CODE, NAME, AGENCY_NIT, ADDRESS, PHONE,
                       MUNICIPALITY_DANE_CODE, EMBOSSER_HIGHLIGHT, EMBOSSER_PINS,
                       CARD_CUSTODIAN_PRIMARY, CARD_CUSTODIAN_PRIMARY_ID,
                       CARD_CUSTODIAN_SECONDARY, CARD_CUSTODIAN_SECONDARY_ID,
                       PIN_CUSTODIAN_PRIMARY, PIN_CUSTODIAN_PRIMARY_ID,
                       PIN_CUSTODIAN_SECONDARY, PIN_CUSTODIAN_SECONDARY_ID,
                       DESCRIPTION, STATUS, CREATED_AT, UPDATED_AT, UPDATED_BY
                  FROM AGENCY WHERE 1=1
                """);
        if (subtypeCode != null) sql.append(" AND SUBTYPE_CODE=:st");
        if (status != null)      sql.append(" AND STATUS=:status");
        if (search != null)      sql.append(" AND (UPPER(NAME) LIKE '%'||UPPER(:q)||'%' OR UPPER(AGENCY_CODE) LIKE '%'||UPPER(:q)||'%')");
        sql.append(" ORDER BY SUBTYPE_CODE, AGENCY_CODE OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY");

        var spec = db.sql(sql.toString());
        if (subtypeCode != null) spec = spec.bind("st", subtypeCode);
        if (status != null)      spec = spec.bind("status", status);
        if (search != null)      spec = spec.bind("q", search);

        return spec.bind("offset", offset)
                .bind("size", s)
                .map(MAPPER)
                .all();
    }

    @Override
    public Mono<Agency> save(Agency a) {
        var spec = db.sql("""
            MERGE INTO AGENCY t
            USING (SELECT :st SUBTYPE_CODE, :ag AGENCY_CODE FROM DUAL) s
               ON (t.SUBTYPE_CODE = s.SUBTYPE_CODE AND t.AGENCY_CODE = s.AGENCY_CODE)
            WHEN MATCHED THEN UPDATE SET
                 t.NAME=:name,
                 t.AGENCY_NIT=:nit,
                 t.ADDRESS=:addr,
                 t.PHONE=:phone,
                 t.MUNICIPALITY_DANE_CODE=:dane,
                 t.EMBOSSER_HIGHLIGHT=:eh,
                 t.EMBOSSER_PINS=:ep,
                 t.CARD_CUSTODIAN_PRIMARY=:ccp,
                 t.CARD_CUSTODIAN_PRIMARY_ID=:ccpid,
                 t.CARD_CUSTODIAN_SECONDARY=:ccs,
                 t.CARD_CUSTODIAN_SECONDARY_ID=:ccsid,
                 t.PIN_CUSTODIAN_PRIMARY=:pcp,
                 t.PIN_CUSTODIAN_PRIMARY_ID=:pcpid,
                 t.PIN_CUSTODIAN_SECONDARY=:pcs,
                 t.PIN_CUSTODIAN_SECONDARY_ID=:pcsid,
                 t.DESCRIPTION=:descr,
                 t.STATUS=:status,
                 t.UPDATED_BY=:by
            WHEN NOT MATCHED THEN INSERT
                 (SUBTYPE_CODE, AGENCY_CODE, NAME, AGENCY_NIT, ADDRESS, PHONE,
                  MUNICIPALITY_DANE_CODE, EMBOSSER_HIGHLIGHT, EMBOSSER_PINS,
                  CARD_CUSTODIAN_PRIMARY, CARD_CUSTODIAN_PRIMARY_ID,
                  CARD_CUSTODIAN_SECONDARY, CARD_CUSTODIAN_SECONDARY_ID,
                  PIN_CUSTODIAN_PRIMARY, PIN_CUSTODIAN_PRIMARY_ID,
                  PIN_CUSTODIAN_SECONDARY, PIN_CUSTODIAN_SECONDARY_ID,
                  DESCRIPTION, STATUS, CREATED_AT, UPDATED_AT, UPDATED_BY)
            VALUES (:st, :ag, :name, :nit, :addr, :phone,
                    :dane, :eh, :ep,
                    :ccp, :ccpid, :ccs, :ccsid,
                    :pcp, :pcpid, :pcs, :pcsid,
                    :descr, :status, SYSTIMESTAMP, SYSTIMESTAMP, :by)
            """)
                .bind("st", a.subtypeCode())
                .bind("ag", a.agencyCode())
                .bind("name", a.name())
                .bind("status", a.status())
                .bind("by", a.updatedBy());

        // binds opcionales
        spec = bindOrNull(spec, "nit", a.agencyNit());
        spec = bindOrNull(spec, "addr", a.address());
        spec = bindOrNull(spec, "phone", a.phone());
        spec = bindOrNull(spec, "dane", a.municipalityDaneCode());
        spec = bindOrNull(spec, "eh", a.embosserHighlight());
        spec = bindOrNull(spec, "ep", a.embosserPins());
        spec = bindOrNull(spec, "ccp", a.cardCustodianPrimary());
        spec = bindOrNull(spec, "ccpid", a.cardCustodianPrimaryId());
        spec = bindOrNull(spec, "ccs", a.cardCustodianSecondary());
        spec = bindOrNull(spec, "ccsid", a.cardCustodianSecondaryId());
        spec = bindOrNull(spec, "pcp", a.pinCustodianPrimary());
        spec = bindOrNull(spec, "pcpid", a.pinCustodianPrimaryId());
        spec = bindOrNull(spec, "pcs", a.pinCustodianSecondary());
        spec = bindOrNull(spec, "pcsid", a.pinCustodianSecondaryId());
        spec = bindOrNull(spec, "descr", a.description());

        return spec.fetch().rowsUpdated().then(findByPk(a.subtypeCode(), a.agencyCode()));
    }

    private DatabaseClient.GenericExecuteSpec bindOrNull(DatabaseClient.GenericExecuteSpec spec, String name, String value) {
        return value == null ? spec.bindNull(name, String.class) : spec.bind(name, value);
    }
}

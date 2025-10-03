package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.persistence;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.function.BiFunction;

@Slf4j
@Repository
public class R2dbcAgencyRepository implements AgencyRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final DatabaseClient db;

    public R2dbcAgencyRepository(DatabaseClient db) {
        this.db = db;
    }

    private static long ms(long t0) {
        return (System.nanoTime() - t0) / 1_000_000;
    }

    private static OffsetDateTime toOffset(Row row, String col) {
        LocalDateTime localDateTime = row.get(col, LocalDateTime.class);
        if (localDateTime != null) {
            return localDateTime.atZone(ZONE).toOffsetDateTime();
        }
        return row.get(col, OffsetDateTime.class);
    }

    private static final BiFunction<Row, RowMetadata, Agency> MAPPER = (row, metadata) ->
            Agency.rehydrate(
                    row.get("subtype_code", String.class),
                    row.get("agency_code", String.class),
                    row.get("name", String.class),
                    row.get("agency_nit", String.class),
                    row.get("address", String.class),
                    row.get("phone", String.class),
                    row.get("municipality_dane_code", String.class),
                    row.get("embosser_highlight", String.class),
                    row.get("embosser_pins", String.class),
                    row.get("card_custodian_primary", String.class),
                    row.get("card_custodian_primary_id", String.class),
                    row.get("card_custodian_secondary", String.class),
                    row.get("card_custodian_secondary_id", String.class),
                    row.get("pin_custodian_primary", String.class),
                    row.get("pin_custodian_primary_id", String.class),
                    row.get("pin_custodian_secondary", String.class),
                    row.get("pin_custodian_secondary_id", String.class),
                    row.get("description", String.class),
                    row.get("status", String.class),
                    toOffset(row, "created_at"),
                    toOffset(row, "updated_at"),
                    row.get("updated_by", String.class)
            );

    @Override
    public Mono<Boolean> existsByPk(String subtypeCode, String agencyCode) {
        long t0 = System.nanoTime();
        log.debug("Repo:AGENCY:existsByPk:start st={} ag={}", subtypeCode, agencyCode);
        return db.sql("""
                SELECT 1 FROM AGENCY
                 WHERE SUBTYPE_CODE=:st AND AGENCY_CODE=:ag
                 FETCH FIRST 1 ROWS ONLY
                """)
                .bind("st", subtypeCode)
                .bind("ag", agencyCode)
                .map((row, metadata) -> 1)
                .first()
                .map(x -> true)
                .defaultIfEmpty(false)
                .doOnSuccess(exists -> log.debug("Repo:AGENCY:existsByPk:done st={} ag={} exists={} elapsedMs={}",
                        subtypeCode, agencyCode, exists, ms(t0)))
                .doOnError(e -> log.warn("Repo:AGENCY:existsByPk:error st={} ag={}", subtypeCode, agencyCode, e));
    }

    @Override
    public Mono<Boolean> existsAnotherActive(String subtypeCode, String excludeAgencyCode) {
        return db.sql("""
            SELECT 1 FROM AGENCY
             WHERE SUBTYPE_CODE=:st AND STATUS='A' AND AGENCY_CODE<>:ag
             FETCH FIRST 1 ROWS ONLY
        """)
                .bind("st", subtypeCode)
                .bind("ag", excludeAgencyCode)
                .map((row, metadata) -> 1)
                .first()
                .map(x -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Agency> findByPk(String subtypeCode, String agencyCode) {
        long t0 = System.nanoTime();
        log.debug("Repo:AGENCY:findByPk:start st={} ag={}", subtypeCode, agencyCode);
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
                .one()
                .doOnSuccess(a -> log.debug("Repo:AGENCY:findByPk:hit st={} ag={} elapsedMs={}",
                        subtypeCode, agencyCode, ms(t0)))
                .doOnError(e -> log.warn("Repo:AGENCY:findByPk:error st={} ag={}", subtypeCode, agencyCode, e));
    }

    @Override
    public Flux<Agency> findAll(String subtypeCode, String status, String search, int page, int size) {
        long t0 = System.nanoTime();
        int p = Math.max(0, page);
        int s = Math.max(1, size);
        int offset = p * s;

        log.info("Repo:AGENCY:findAll:start st={} status={} page={} size={} offset={}",
                subtypeCode, status, p, s, offset);

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
        if (subtypeCode != null) {
            sql.append(" AND SUBTYPE_CODE=:st");
        }
        if (status != null) {
            sql.append(" AND STATUS=:status");
        }
        if (search != null) {
            sql.append(" AND (UPPER(NAME) LIKE '%'||UPPER(:q)||'%' OR UPPER(AGENCY_CODE) LIKE '%'||UPPER(:q)||'%')");
        }
        sql.append(" ORDER BY SUBTYPE_CODE, AGENCY_CODE OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY");

        var spec = db.sql(sql.toString());
        if (subtypeCode != null) {
            spec = spec.bind("st", subtypeCode);
        }
        if (status != null) {
            spec = spec.bind("status", status);
        }
        if (search != null) {
            spec = spec.bind("q", search);
        }

        return spec.bind("offset", offset)
                .bind("size", s)
                .map(MAPPER)
                .all()
                .doOnComplete(() -> log.info("Repo:AGENCY:findAll:done st={} status={} page={} size={} elapsedMs={}",
                        subtypeCode, status, p, s, ms(t0)))
                .doOnError(e -> log.warn("Repo:AGENCY:findAll:error st={} status={} page={} size={}",
                        subtypeCode, status, p, s, e));
    }

    @Override
    public Mono<Agency> save(Agency a) {
        long t0 = System.nanoTime();
        log.debug("Repo:AGENCY:save:start st={} ag={}", a.subtypeCode(), a.agencyCode());

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
                .bind("status", a.status());

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
        spec = (a.updatedBy() != null)
                ? spec.bind("by", a.updatedBy())
                : spec.bindNull("by", String.class);

        return spec.fetch()
                .rowsUpdated()
                .doOnNext(n -> log.debug("Repo:AGENCY:save:merge rowsUpdated={} st={} ag={}", n, a.subtypeCode(), a.agencyCode()))
                .then(findByPk(a.subtypeCode(), a.agencyCode()))
                .doOnSuccess(x -> log.info("Repo:AGENCY:save:done st={} ag={} elapsedMs={}",
                        x.subtypeCode(), x.agencyCode(), ms(t0)))
                .doOnError(e -> log.warn("Repo:AGENCY:save:error st={} ag={}", a.subtypeCode(), a.agencyCode(), e));
    }

    private DatabaseClient.GenericExecuteSpec bindOrNull(DatabaseClient.GenericExecuteSpec spec, String name, String value) {
        return value == null ? spec.bindNull(name, String.class) : spec.bind(name, value);
    }
}

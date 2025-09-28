package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.rule;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
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
public class R2dbcValidationMapRepository implements ValidationMapRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final DatabaseClient db;

    public R2dbcValidationMapRepository(DatabaseClient db) {
        this.db = db;
    }

    private static long ms(long t0) {
        return (System.nanoTime() - t0) / 1_000_000;
    }

    private static OffsetDateTime toOffset(Row r, String col) {
        LocalDateTime ldt = r.get(col, LocalDateTime.class);
        return ldt != null ? ldt.atZone(ZONE).toOffsetDateTime() : r.get(col, OffsetDateTime.class);
    }

    private static final BiFunction<Row, RowMetadata, ValidationMap> MAP_MAPPER = (r, m) ->
            ValidationMap.rehydrate(
                    r.get("subtype_val_map_id", Long.class),
                    r.get("subtype_code", String.class),
                    r.get("bin", String.class),
                    r.get("validation_id", Long.class),
                    r.get("status", String.class),
                    r.get("value_flag", String.class),
                    r.get("value_num", Double.class),
                    r.get("value_text", String.class),
                    toOffset(r, "created_at"),
                    toOffset(r, "updated_at"),
                    r.get("updated_by", String.class)
            );


    @Override
    public Mono<Boolean> existsActive(String subtypeCode, String bin, Long validationId) {
        long t0 = System.nanoTime();
        return db.sql("""
                        SELECT 1 FROM SUBTYPE_VALIDATION_MAP
                         WHERE SUBTYPE_CODE=:sc AND BIN=:be AND VALIDATION_ID=:vid AND STATUS='A'
                         FETCH FIRST 1 ROWS ONLY
                        """)
                .bind("sc", subtypeCode).bind("be", bin).bind("vid", validationId)
                .fetch().first().map(m -> true).defaultIfEmpty(false)
                .doOnSuccess(b -> log.debug("Repo:Rule:existsActive:st={} bin={} vid={} exists={} elapsedMs={}",
                        subtypeCode, bin, validationId, b, (System.nanoTime() - t0) / 1_000_000));
    }


    @Override
    public Mono<ValidationMap> save(ValidationMap map) {
        long t0 = System.nanoTime();
        var spec = db.sql("""
                        MERGE INTO SUBTYPE_VALIDATION_MAP t
                        USING (
                          SELECT s.SUBTYPE_CODE, s.BIN, v.VALIDATION_ID
                            FROM SUBTYPE s
                            JOIN SUBTYPE_VALIDATION v ON v.VALIDATION_ID = :vid
                           WHERE s.SUBTYPE_CODE = :sc
                             AND s.BIN = :be
                             AND v.STATUS = 'A'
                             AND v.VALID_FROM <= SYSTIMESTAMP
                             AND (v.VALID_TO IS NULL OR v.VALID_TO >= SYSTIMESTAMP)
                        ) src
                           ON (t.SUBTYPE_CODE  = src.SUBTYPE_CODE
                           AND t.BIN           = src.BIN
                           AND t.VALIDATION_ID = src.VALIDATION_ID)
                        WHEN MATCHED THEN UPDATE SET
                             t.STATUS     = :st,
                             t.VALUE_FLAG = :vf,
                             t.VALUE_NUM  = :vn,
                             t.VALUE_TEXT = :vt,
                             t.UPDATED_AT = SYSTIMESTAMP,
                             t.UPDATED_BY = :by
                        WHEN NOT MATCHED THEN INSERT
                             (SUBTYPE_VAL_MAP_ID, SUBTYPE_CODE, BIN, VALIDATION_ID,
                              STATUS, VALUE_FLAG, VALUE_NUM, VALUE_TEXT,
                              CREATED_AT, UPDATED_AT, UPDATED_BY)
                        VALUES
                             (SEQ_SUBTYPE_VAL_MAP_ID.NEXTVAL, src.SUBTYPE_CODE, src.BIN, src.VALIDATION_ID,
                              :st, :vf, :vn, :vt,
                              SYSTIMESTAMP, SYSTIMESTAMP, :by)
                        """)
                .bind("sc", map.subtypeCode())
                .bind("be", map.bin())
                .bind("vid", map.validationId())
                .bind("st", map.status());

        spec = (map.valueFlag() != null) ? spec.bind("vf", map.valueFlag()) : spec.bindNull("vf", String.class);
        spec = (map.valueNum() != null) ? spec.bind("vn", map.valueNum()) : spec.bindNull("vn", Double.class);
        spec = (map.valueText() != null) ? spec.bind("vt", map.valueText()) : spec.bindNull("vt", String.class);
        spec = (map.updatedBy() != null) ? spec.bind("by", map.updatedBy()) : spec.bindNull("by", String.class);

        return spec.fetch().rowsUpdated()
                .doOnNext(n -> log.debug("Repo:Rule:save:merge rowsUpdated={} st={} bin={} vid={}",
                        n, map.subtypeCode(), map.bin(), map.validationId()))
                .flatMap(rows -> rows > 0
                        ? findByNaturalKey(map.subtypeCode(), map.bin(), map.validationId())
                        : Mono.error(new java.util.NoSuchElementException(
                        "No existe SUBTYPE(subtypeCode,bin) o la VALIDATION no está activa/vigente")))
                .doOnSuccess(x -> log.info("Repo:Rule:save:done st={} bin={} vid={} elapsedMs={}",
                        x.subtypeCode(), x.bin(), x.validationId(), (System.nanoTime() - t0) / 1_000_000));
    }


    @Override
    public Mono<ValidationMap> findByNaturalKey(String subtypeCode, String bin, Long validationId) {
        long t0 = System.nanoTime();
        return db.sql("""
                        SELECT SUBTYPE_VAL_MAP_ID, SUBTYPE_CODE, BIN, VALIDATION_ID,
                               STATUS, VALUE_FLAG, VALUE_NUM, VALUE_TEXT,
                               CREATED_AT, UPDATED_AT, UPDATED_BY
                          FROM SUBTYPE_VALIDATION_MAP
                         WHERE SUBTYPE_CODE=:sc AND BIN=:be AND VALIDATION_ID=:vid
                        """)
                .bind("sc", subtypeCode)
                .bind("be", bin)
                .bind("vid", validationId)
                .map(MAP_MAPPER)
                .one()
                .doOnSuccess(m -> log.info("Repo:Rule:findByNaturalKey:ok st={} bin={} vid={} elapsedMs={}",
                        subtypeCode, bin, validationId, ms(t0)))
                .doOnError(e -> log.warn("Repo:Rule:findByNaturalKey:err st={} bin={} vid={} msg={}",
                        subtypeCode, bin, validationId, e.getMessage()));
    }


    @Override
    public Flux<ValidationMap> findAll(String subtypeCode, String bin, String status, int page, int size) {
        long t0 = System.nanoTime();
        int p = Math.max(0, page), s = Math.max(1, size), off = p * s;

        var sb = new StringBuilder("""
                SELECT SUBTYPE_VAL_MAP_ID, SUBTYPE_CODE, BIN, VALIDATION_ID,
                       STATUS, VALUE_FLAG, VALUE_NUM, VALUE_TEXT,
                       CREATED_AT, UPDATED_AT, UPDATED_BY
                  FROM SUBTYPE_VALIDATION_MAP WHERE 1=1
                """);

        if (subtypeCode != null) sb.append(" AND SUBTYPE_CODE=:sc");
        if (bin != null) sb.append(" AND BIN=:be");
        if (status != null) sb.append(" AND STATUS=:st");

        sb.append(" ORDER BY SUBTYPE_CODE, BIN, VALIDATION_ID OFFSET :off ROWS FETCH NEXT :sz ROWS ONLY");

        var spec = db.sql(sb.toString());
        if (subtypeCode != null) spec = spec.bind("sc", subtypeCode);
        if (bin != null) spec = spec.bind("be", bin);
        if (status != null) spec = spec.bind("st", status);

        return spec.bind("off", off).bind("sz", s).map(MAP_MAPPER).all()
                .doOnSubscribe(su -> log.info("Repo:Rule:findAll:recv st={} bin={} status={} page={} size={}",
                        subtypeCode, bin, status, page, size))
                .doOnComplete(() -> log.info("Repo:Rule:findAll:done elapsedMs={}", ms(t0)));
    }

    @Override
    public Flux<ValidationMap> findResolved(String subtypeCode, String bin, String status, int page, int size) {
        long t0 = System.nanoTime();
        int p = Math.max(0, page), s = Math.max(1, size);

        var sb = new StringBuilder("""
                SELECT
                   m.SUBTYPE_VAL_MAP_ID,
                   m.SUBTYPE_CODE,
                   m.BIN,
                   m.VALIDATION_ID,
                   m.STATUS,
                   m.VALUE_FLAG,
                   m.VALUE_NUM,
                   m.VALUE_TEXT,
                   m.CREATED_AT,
                   m.UPDATED_AT,
                   m.UPDATED_BY
                FROM SUBTYPE_VALIDATION_MAP m
                JOIN SUBTYPE_VALIDATION v ON v.VALIDATION_ID = m.VALIDATION_ID
                WHERE m.SUBTYPE_CODE=:sc AND m.BIN=:be
                """);
        if (status != null) sb.append(" AND m.STATUS=:mst");
        sb.append(" ORDER BY m.SUBTYPE_CODE, m.BIN, m.VALIDATION_ID OFFSET :off ROWS FETCH NEXT :sz ROWS ONLY");

        var spec = db.sql(sb.toString())
                .bind("sc", subtypeCode)
                .bind("be", bin)
                .bind("off", p * s)
                .bind("sz", s);
        if (status != null) spec = spec.bind("mst", status);

        return spec.map(MAP_MAPPER).all()
                .doOnSubscribe(su -> log.info("Repo:Rule:findResolved:recv st={} bin={} status={} page={} size={}",
                        subtypeCode, bin, status, page, size))
                .doOnComplete(() -> log.info("Repo:Rule:findResolved:done elapsedMs={}", ms(t0)));
    }
}
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.rule;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
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
public class R2dbcValidationMapRepository implements ValidationMapRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final DatabaseClient db;

    public R2dbcValidationMapRepository(DatabaseClient db) {
        this.db = db;
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
        return db.sql("""
                        SELECT 1 FROM SUBTYPE_VALIDATION_MAP
                         WHERE SUBTYPE_CODE=:sc AND BIN=:be AND VALIDATION_ID=:vid AND STATUS='A'
                         FETCH FIRST 1 ROWS ONLY
                        """)
                .bind("sc", subtypeCode).bind("be", bin).bind("vid", validationId)
                .fetch().first().map(m -> true).defaultIfEmpty(false);
    }

    /**
     * Inserta/actualiza por VALIDATION_ID (se asume que ya fue validado antes).
     * Mantiene el MERGE con JOIN para robustez ante condiciones de carrera.
     */
    @Override
    public Mono<ValidationMap> save(ValidationMap map) {
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
                .bind("sc",  map.subtypeCode())
                .bind("be",  map.bin())
                .bind("vid", map.validationId())
                .bind("st",  map.status())
                .bind("by",  map.updatedBy());


        spec = (map.valueFlag() != null) ? spec.bind("vf", map.valueFlag())
                : spec.bindNull("vf", String.class);

        spec = (map.valueNum()  != null) ? spec.bind("vn", map.valueNum())
                : spec.bindNull("vn", Double.class);

        spec = (map.valueText() != null) ? spec.bind("vt", map.valueText())
                : spec.bindNull("vt", String.class);

        return spec.fetch().rowsUpdated()
                .flatMap(rows -> rows > 0
                        ? findByNaturalKey(map.subtypeCode(), map.bin(), map.validationId())
                        : Mono.error(new java.util.NoSuchElementException(
                        "No existe SUBTYPE(subtypeCode,bin) o la VALIDATION no está activa/vigente")));
    }



    @Override
    public Mono<ValidationMap> findByNaturalKey(String subtypeCode, String bin, Long validationId) {
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
                .one();
    }

    @Override
    public Flux<ValidationMap> findAll(String subtypeCode, String bin, String status, int page, int size) {
        int p = Math.max(0, page), s = Math.max(1, size), off = p * s;

        var sb = new StringBuilder("""
        SELECT SUBTYPE_VAL_MAP_ID, SUBTYPE_CODE, BIN, VALIDATION_ID,
               STATUS, VALUE_FLAG, VALUE_NUM, VALUE_TEXT,
               CREATED_AT, UPDATED_AT, UPDATED_BY
          FROM SUBTYPE_VALIDATION_MAP WHERE 1=1
        """);

        if (subtypeCode != null) sb.append(" AND SUBTYPE_CODE=:sc");
        if (bin != null)         sb.append(" AND BIN=:be");
        if (status != null)      sb.append(" AND STATUS=:st");

        // Elige un ORDER BY válido (por ej. por fechas y claves naturales)
        sb.append(" ORDER BY SUBTYPE_CODE, BIN, VALIDATION_ID OFFSET :off ROWS FETCH NEXT :sz ROWS ONLY");

        var spec = db.sql(sb.toString());
        if (subtypeCode != null) spec = spec.bind("sc", subtypeCode);
        if (bin != null)         spec = spec.bind("be", bin);
        if (status != null)      spec = spec.bind("st", status);

        return spec.bind("off", off).bind("sz", s).map(MAP_MAPPER).all();
    }


    @Override
    public Flux<ValidationMap> findResolved(String subtypeCode, String bin, String status, int page, int size) {
        int p = Math.max(0, page), s = Math.max(1, size);

        var sb = new StringBuilder("""
                    SELECT v.VALIDATION_ID, v.CODE,m.BIN, v.VALUE_FLAG, v.VALUE_NUM, v.VALUE_TEXT,
                           v.STATUS, v.CREATED_AT, v.UPDATED_AT,
                           m.UPDATED_BY
                      FROM SUBTYPE_VALIDATION_MAP m
                      JOIN SUBTYPE_VALIDATION v ON v.VALIDATION_ID = m.VALIDATION_ID
                     WHERE m.SUBTYPE_CODE=:sc AND m.BIN=:be
                """);
        if (status != null) sb.append(" AND m.STATUS=:mst");
        sb.append(" ORDER BY  OFFSET :off ROWS FETCH NEXT :sz ROWS ONLY");

        var spec = db.sql(sb.toString())
                .bind("sc", subtypeCode)
                .bind("be", bin)
                .bind("off", p * s)
                .bind("sz", s);
        if (status != null) spec = spec.bind("mst", status);


        return spec.map((r, m) -> ValidationMap.rehydrate(
                r.get("validation_id", Long.class),
                r.get("code", String.class),
                r.get("bin", String.class),
                r.get("validationId", long.class),
                r.get("status", String.class),
                r.get("value_flag", String.class),
                r.get("value_num", Double.class),
                r.get("value_text", String.class),
                toOffset(r, "created_at"),
                toOffset(r, "updated_at"),
                r.get("updated_by", String.class)
        )).all();
    }
}
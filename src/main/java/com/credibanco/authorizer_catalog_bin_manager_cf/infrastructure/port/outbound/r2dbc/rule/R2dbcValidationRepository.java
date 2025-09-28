package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.rule;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
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
public class R2dbcValidationRepository implements ValidationRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final DatabaseClient db;
    public R2dbcValidationRepository(DatabaseClient db) { this.db = db; }
    private static long ms(long t0) { return (System.nanoTime()-t0)/1_000_000; }

    private static OffsetDateTime toOffset(Row r, String col) {
        LocalDateTime ldt = r.get(col, LocalDateTime.class);
        return ldt != null ? ldt.atZone(ZONE).toOffsetDateTime() : r.get(col, OffsetDateTime.class);
    }

    private static final BiFunction<Row, RowMetadata, Validation> MAPPER = (r,m) ->
            Validation.rehydrate(
                    r.get("validation_id", Long.class),
                    r.get("code", String.class),
                    r.get("description", String.class),
                    ValidationDataType.valueOf(r.get("data_type", String.class)),
                    r.get("status", String.class),
                    toOffset(r,"valid_from"),
                    toOffset(r,"valid_to"),
                    toOffset(r,"created_at"),
                    toOffset(r,"updated_at"),
                    r.get("updated_by", String.class)
            );

    @Override
    public Mono<Boolean> existsByCode(String code) {
        long t0 = System.nanoTime();
        return db.sql("SELECT 1 FROM SUBTYPE_VALIDATION WHERE CODE=:c FETCH FIRST 1 ROWS ONLY")
                .bind("c", code)
                .fetch().first().map(m -> true).defaultIfEmpty(false)
                .doOnSuccess(exists -> log.debug("Repo:Validation:existsByCode code={} exists={} elapsedMs={}",
                        code, exists, ms(t0)))
                .doOnError(e -> log.warn("Repo:Validation:existsByCode:err code={} msg={}", code, e.getMessage()));
    }

    @Override
    public Mono<Validation> save(Validation v) {
        long t0 = System.nanoTime();
        var spec = db.sql("""
        MERGE INTO SUBTYPE_VALIDATION t
        USING (SELECT :code CODE FROM DUAL) s
           ON (t.CODE = s.CODE)
        WHEN MATCHED THEN UPDATE SET
             t.DESCRIPTION=:descr,
             t.DATA_TYPE=:type,
             t.STATUS=:status,
             t.VALID_TO=:vt,
             t.UPDATED_AT=SYSTIMESTAMP,
             t.UPDATED_BY=:by
        WHEN NOT MATCHED THEN INSERT
             (VALIDATION_ID, CODE, DESCRIPTION, DATA_TYPE,
              STATUS, VALID_FROM, VALID_TO, CREATED_AT, UPDATED_AT, UPDATED_BY)
        VALUES (SEQ_SUBTIPO_VALIDATION_ID.NEXTVAL, :code, :descr, :type,
                :status, NVL(:vf, SYSTIMESTAMP), :vt, SYSTIMESTAMP, SYSTIMESTAMP, :by)
        """)
                .bind("code", v.code())
                .bind("descr", v.description())
                .bind("type", v.dataType().name())
                .bind("status", v.status());

        spec = (v.validFrom()!=null) ? spec.bind("vf", v.validFrom()) : spec.bindNull("vf", java.time.OffsetDateTime.class);
        spec = (v.validTo()!=null)   ? spec.bind("vt", v.validTo())   : spec.bindNull("vt", java.time.OffsetDateTime.class);
        spec = (v.updatedBy()!=null) ? spec.bind("by", v.updatedBy()) : spec.bindNull("by", String.class);

        return spec.fetch().rowsUpdated()
                .doOnNext(n -> log.debug("Repo:Validation:save:rowsUpdated={} code={}", n, v.code()))
                .then(findByCode(v.code()))
                .doOnSuccess(x -> log.info("Repo:Validation:save:done code={} elapsedMs={}", x.code(),
                        (System.nanoTime()-t0)/1_000_000));
    }


    @Override
    public Mono<Validation> findByCode(String code) {
        long t0 = System.nanoTime();
        return db.sql("""
            SELECT VALIDATION_ID, CODE, DESCRIPTION, DATA_TYPE,
                   STATUS, VALID_FROM, VALID_TO, CREATED_AT, UPDATED_AT, UPDATED_BY
              FROM SUBTYPE_VALIDATION WHERE CODE=:c
            """)
                .bind("c", code)
                .map(MAPPER)
                .one()
                .doOnSuccess(v -> log.info("Repo:Validation:findByCode:ok code={} elapsedMs={}",
                        code, ms(t0)))
                .doOnError(e -> log.warn("Repo:Validation:findByCode:err code={} msg={}", code, e.getMessage()));
    }

    @Override
    public Mono<Validation> findById(Long id) {
        long t0 = System.nanoTime();
        return db.sql("""
            SELECT VALIDATION_ID, CODE, DESCRIPTION, DATA_TYPE,
                   STATUS, VALID_FROM, VALID_TO, CREATED_AT, UPDATED_AT, UPDATED_BY
              FROM SUBTYPE_VALIDATION WHERE VALIDATION_ID=:id
            """)
                .bind("id", id)
                .map(MAPPER)
                .one()
                .doOnSuccess(v -> log.info("Repo:Validation:findById:ok id={} code={} elapsedMs={}",
                        id, v.code(), ms(t0)))
                .doOnError(e -> log.warn("Repo:Validation:findById:err id={} msg={}", id, e.getMessage()));
    }

    @Override
    public Flux<Validation> findAll(String status, String search, int page, int size) {
        long t0 = System.nanoTime();
        int p = Math.max(0, page), s = Math.max(1, size), off = p * s;

        var sb = new StringBuilder("""
            SELECT VALIDATION_ID, CODE, DESCRIPTION, DATA_TYPE,
                   STATUS, VALID_FROM, VALID_TO, CREATED_AT, UPDATED_AT, UPDATED_BY
              FROM SUBTYPE_VALIDATION WHERE 1=1
            """);
        if (status != null) sb.append(" AND STATUS=:st");
        if (search != null) sb.append(" AND (UPPER(CODE) LIKE '%'||UPPER(:q)||'%' OR UPPER(DESCRIPTION) LIKE '%'||UPPER(:q)||'%')");
        sb.append(" ORDER BY CODE OFFSET :off ROWS FETCH NEXT :sz ROWS ONLY");

        var spec = db.sql(sb.toString());
        if (status != null) spec = spec.bind("st", status);
        if (search != null) spec = spec.bind("q", search);

        return spec.bind("off", off).bind("sz", s).map(MAPPER).all()
                .doOnSubscribe(su -> log.info("Repo:Validation:findAll:recv status={} q={} page={} size={}",
                        status, search, page, size))
                .doOnComplete(() -> log.info("Repo:Validation:findAll:done elapsedMs={}", ms(t0)));
    }
}

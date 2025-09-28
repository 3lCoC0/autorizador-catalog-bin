package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.plan;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
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
public class R2dbcCommercePlanRepository implements CommercePlanRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final DatabaseClient db;

    public R2dbcCommercePlanRepository(DatabaseClient db) { this.db = db; }

    private static long ms(long t0) { return (System.nanoTime() - t0) / 1_000_000; }

    private static OffsetDateTime toOffset(Row r, String col) {
        LocalDateTime ldt = r.get(col, LocalDateTime.class);
        return ldt != null ? ldt.atZone(ZONE).toOffsetDateTime() : r.get(col, OffsetDateTime.class);
    }

    private static final BiFunction<Row, RowMetadata, CommercePlan> MAPPER = (r, m) ->
            CommercePlan.rehydrate(
                    r.get("plan_id", Long.class),
                    r.get("plan_code", String.class),
                    r.get("plan_name", String.class),
                    CommerceValidationMode.valueOf(r.get("validation_mode", String.class)),
                    r.get("description", String.class),
                    r.get("status", String.class),
                    toOffset(r,"created_at"),
                    toOffset(r,"updated_at"),
                    r.get("updated_by", String.class)
            );

    @Override
    public Mono<Boolean> existsByCode(String code) {
        long t0 = System.nanoTime();
        log.debug("Repo:PLAN:existsByCode:start code={}", code);
        return db.sql("""
                SELECT 1 FROM COMMERCE_PLAN
                 WHERE PLAN_CODE=:c
                 FETCH FIRST 1 ROWS ONLY
            """).bind("c", code)
                .fetch().first()
                .map(m -> true)
                .defaultIfEmpty(false)
                .doOnSuccess(exists -> log.debug("Repo:PLAN:existsByCode:done code={} exists={} elapsedMs={}",
                        code, exists, ms(t0)))
                .doOnError(e -> log.warn("Repo:PLAN:existsByCode:error code={}", code, e));
    }

    @Override
    public Mono<CommercePlan> findByCode(String code) {
        long t0 = System.nanoTime();
        log.debug("Repo:PLAN:findByCode:start code={}", code);
        return db.sql("""
            SELECT PLAN_ID, PLAN_CODE, PLAN_NAME, VALIDATION_MODE, DESCRIPTION, STATUS,
                   CREATED_AT, UPDATED_AT, UPDATED_BY
              FROM COMMERCE_PLAN WHERE PLAN_CODE=:c
        """).bind("c", code)
                .map(MAPPER).one()
                .doOnSuccess(p -> log.debug("Repo:PLAN:findByCode:hit code={} elapsedMs={}", code, ms(t0)))
                .doOnError(e -> log.warn("Repo:PLAN:findByCode:error code={}", code, e));
    }

    @Override
    public Flux<CommercePlan> findAll(String status, String q, int page, int size) {
        long t0 = System.nanoTime();
        int p = Math.max(0, page);
        int s = Math.max(1, size);
        int off = p * s;
        log.info("Repo:PLAN:findAll:start status={} q={} page={} size={} offset={}", status, q, p, s, off);

        var sb = new StringBuilder("""
            SELECT PLAN_ID, PLAN_CODE, PLAN_NAME, VALIDATION_MODE, DESCRIPTION, STATUS,
                   CREATED_AT, UPDATED_AT, UPDATED_BY
              FROM COMMERCE_PLAN WHERE 1=1
        """);
        if (status != null) sb.append(" AND STATUS=:st");
        if (q != null)      sb.append(" AND (UPPER(PLAN_CODE) LIKE '%'||UPPER(:q)||'%' OR UPPER(PLAN_NAME) LIKE '%'||UPPER(:q)||'%')");
        sb.append(" ORDER BY PLAN_CODE OFFSET :off ROWS FETCH NEXT :sz ROWS ONLY");

        var spec = db.sql(sb.toString()).bind("off", off).bind("sz", s);
        if (status != null) spec = spec.bind("st", status);
        if (q != null)      spec = spec.bind("q", q);

        return spec.map(MAPPER).all()
                .doOnComplete(() -> log.info("Repo:PLAN:findAll:done page={} size={} elapsedMs={}", p, s, ms(t0)))
                .doOnError(e -> log.warn("Repo:PLAN:findAll:error page={} size={}", p, s, e));
    }

    @Override
    public Mono<CommercePlan> save(CommercePlan p) {
        long t0 = System.nanoTime();
        log.debug("Repo:PLAN:save:start code={}", p.code());

        var spec = db.sql("""
            MERGE INTO COMMERCE_PLAN t
            USING (SELECT :code PLAN_CODE FROM DUAL) s
               ON (t.PLAN_CODE = s.PLAN_CODE)
            WHEN MATCHED THEN UPDATE SET
                 t.PLAN_NAME=:name,
                 t.DESCRIPTION=:descr,
                 t.STATUS=:st,
                 t.UPDATED_AT=SYSTIMESTAMP,
                 t.UPDATED_BY=:by,
                 t.VALIDATION_MODE=:mode
            WHEN NOT MATCHED THEN INSERT
                 (PLAN_ID, PLAN_CODE, PLAN_NAME, VALIDATION_MODE, DESCRIPTION,
                  STATUS, CREATED_AT, UPDATED_AT, UPDATED_BY)
            VALUES (SEQ_COMMERCE_PLAN_ID.NEXTVAL, :code, :name, :mode, :descr,
                    :st, SYSTIMESTAMP, SYSTIMESTAMP, :by)
        """)
                .bind("code", p.code())
                .bind("name", p.name())
                .bind("st", p.status())
                .bind("mode", p.validationMode().name());

        spec = (p.description() == null) ? spec.bindNull("descr", String.class) : spec.bind("descr", p.description());
        spec = (p.updatedBy() == null)   ? spec.bindNull("by", String.class)    : spec.bind("by", p.updatedBy());

        return spec.fetch().rowsUpdated()
                .doOnNext(n -> log.debug("Repo:PLAN:merge rowsUpdated={} code={}", n, p.code()))
                .then(findByCode(p.code()))
                .doOnSuccess(plan -> log.info("Repo:PLAN:save:done code={} elapsedMs={}", plan.code(), ms(t0)))
                .doOnError(e -> log.warn("Repo:PLAN:save:error code={}", p.code(), e));
    }
}

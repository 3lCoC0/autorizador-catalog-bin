package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.plan;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.r2dbc.spi.Row; import io.r2dbc.spi.RowMetadata;
import java.time.LocalDateTime; import java.time.OffsetDateTime; import java.time.ZoneId;
import java.util.function.BiFunction;

@Repository
public class R2dbcCommercePlanRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final DatabaseClient db;
    public R2dbcCommercePlanRepository(DatabaseClient db) { this.db = db; }

    private static OffsetDateTime toOffset(Row r, String col) {
        LocalDateTime ldt = r.get(col, LocalDateTime.class);
        return ldt != null ? ldt.atZone(ZONE).toOffsetDateTime() : r.get(col, OffsetDateTime.class);
    }

    private static final BiFunction<Row, RowMetadata, CommercePlan> MAPPER = (r,m) ->
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

    public Mono<Boolean> existsByCode(String code) {
        return db.sql("SELECT 1 FROM COMMERCE_PLAN WHERE PLAN_CODE=:c FETCH FIRST 1 ROWS ONLY")
                .bind("c", code).fetch().first().map(m->true).defaultIfEmpty(false);
    }

    public Mono<CommercePlan> save(CommercePlan p) {
        var spec = db.sql("""
            MERGE INTO COMMERCE_PLAN t
            USING (SELECT :code PLAN_CODE FROM DUAL) s
               ON (t.PLAN_CODE = s.PLAN_CODE)
            WHEN MATCHED THEN UPDATE SET
                 t.PLAN_NAME=:name, t.DESCRIPTION=:descr,
                 t.STATUS=:st, t.UPDATED_AT=SYSTIMESTAMP, t.UPDATED_BY=:by
            WHEN NOT MATCHED THEN INSERT
                 (PLAN_ID, PLAN_CODE, PLAN_NAME, VALIDATION_MODE, DESCRIPTION, STATUS, CREATED_AT, UPDATED_AT, UPDATED_BY)
            VALUES (SEQ_COMMERCE_PLAN_ID.NEXTVAL, :code, :name, :mode, :descr, :st, SYSTIMESTAMP, SYSTIMESTAMP, :by)
            """)
                .bind("code", p.code())
                .bind("name", p.name())
                .bind("descr", p.description())
                .bind("st", p.status())
                .bind("by", p.updatedBy())
                .bind("mode", p.validationMode().name());

        return spec.fetch().rowsUpdated().then(findByCode(p.code()));
    }

    public Mono<CommercePlan> findByCode(String code) {
        return db.sql("""
            SELECT PLAN_ID, PLAN_CODE, PLAN_NAME, VALIDATION_MODE, DESCRIPTION, STATUS,
                   CREATED_AT, UPDATED_AT, UPDATED_BY
              FROM COMMERCE_PLAN WHERE PLAN_CODE=:c
        """).bind("c", code).map(MAPPER).one();
    }

    public Flux<CommercePlan> findAll(String status, String q, int page, int size) {
        int off = Math.max(0,page) * Math.max(1,size);
        var sb = new StringBuilder("""
            SELECT PLAN_ID, PLAN_CODE, PLAN_NAME, VALIDATION_MODE, DESCRIPTION, STATUS,
                   CREATED_AT, UPDATED_AT, UPDATED_BY
              FROM COMMERCE_PLAN WHERE 1=1
        """);
        if (status != null) sb.append(" AND STATUS=:st");
        if (q != null) sb.append(" AND (UPPER(PLAN_CODE) LIKE '%'||UPPER(:q)||'%' OR UPPER(PLAN_NAME) LIKE '%'||UPPER(:q)||'%')");
        sb.append(" ORDER BY PLAN_CODE OFFSET :off ROWS FETCH NEXT :sz ROWS ONLY");

        var spec = db.sql(sb.toString()).bind("off", off).bind("sz", Math.max(1,size));
        if (status != null) spec = spec.bind("st", status);
        if (q != null) spec = spec.bind("q", q);
        return spec.map(MAPPER).all();
    }
}

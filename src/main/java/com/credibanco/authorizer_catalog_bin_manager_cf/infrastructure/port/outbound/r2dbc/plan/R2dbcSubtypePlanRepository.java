package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.plan;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.function.BiFunction;

@Repository
public class R2dbcSubtypePlanRepository implements SubtypePlanRepository {
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final DatabaseClient db;
    public R2dbcSubtypePlanRepository(DatabaseClient db) { this.db = db; }

    private static OffsetDateTime toOffset(Row r, String col) {
        LocalDateTime ldt = r.get(col, LocalDateTime.class);
        return ldt != null ? ldt.atZone(ZONE).toOffsetDateTime() : r.get(col, OffsetDateTime.class);
    }

    private static final BiFunction<Row, RowMetadata, SubtypePlanLink> MAPPER = (r,m) ->
            SubtypePlanLink.rehydrate(
                    r.get("subtype_code", String.class),
                    r.get("plan_id", Long.class),
                    toOffset(r,"created_at"),
                    toOffset(r,"updated_at"),
                    r.get("updated_by", String.class)
            );

    @Override
    public Mono<Integer> upsert(String subtypeCode, Long planId, String by) {
        return db.sql("""
        MERGE INTO SUBTYPE_COMMERCE_PLAN t
        USING (SELECT :sc SUBTYPE_CODE FROM DUAL) s
           ON (t.SUBTYPE_CODE = s.SUBTYPE_CODE)
        WHEN MATCHED THEN UPDATE SET
             t.PLAN_ID=:pid, t.UPDATED_AT=SYSTIMESTAMP, t.UPDATED_BY=:by
        WHEN NOT MATCHED THEN INSERT
             (SUBTYPE_PLAN_ID, SUBTYPE_CODE, PLAN_ID, CREATED_AT, UPDATED_AT, UPDATED_BY)
        VALUES (SEQ_SUBTYPE_COMMERCE_PLAN_ID.NEXTVAL, :sc, :pid, SYSTIMESTAMP, SYSTIMESTAMP, :by)
    """)
                .bind("sc", subtypeCode)
                .bind("pid", planId)
                .bind("by", by)
                .fetch()
                .rowsUpdated()              // Mono<Long>
                .map(Long::intValue);       // -> Mono<Integer>
    }

    @Override
    public Mono<SubtypePlanLink> findBySubtype(String subtypeCode) { // ✅ nombre unificado
        return db.sql("""
            SELECT SUBTYPE_CODE, PLAN_ID, CREATED_AT, UPDATED_AT, UPDATED_BY
              FROM SUBTYPE_COMMERCE_PLAN
             WHERE SUBTYPE_CODE=:sc
        """).bind("sc", subtypeCode).map(MAPPER).one();
    }
}

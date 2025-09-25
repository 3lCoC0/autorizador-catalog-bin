package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.plan;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcSubtypePlanRepository {
    private final DatabaseClient db;
    public R2dbcSubtypePlanRepository(DatabaseClient db) { this.db = db; }

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
        """).bind("sc", subtypeCode).bind("pid", planId).bind("by", by)
                .fetch().rowsUpdated();
    }
}

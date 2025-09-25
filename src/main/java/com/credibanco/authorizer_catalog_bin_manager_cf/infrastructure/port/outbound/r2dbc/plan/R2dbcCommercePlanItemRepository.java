package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.plan;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux; import reactor.core.publisher.Mono;

@Repository
public class R2dbcCommercePlanItemRepository {
    private final DatabaseClient db;
    public R2dbcCommercePlanItemRepository(DatabaseClient db) { this.db = db; }

    public Mono<Integer> insertMcc(Long planId, String mcc, String by) {
        return db.sql("""
            INSERT INTO COMMERCE_PLAN_ITEM (PLAN_ITEM_ID, PLAN_ID, MCC, MERCHANT_ID, CREATED_AT, UPDATED_AT, UPDATED_BY)
            VALUES (SEQ_COMMERCE_PLAN_ITEM_ID.NEXTVAL, :pid, :mcc, NULL, SYSTIMESTAMP, SYSTIMESTAMP, :by)
        """).bind("pid", planId).bind("mcc", mcc).bind("by", by).fetch().rowsUpdated();
    }

    public Mono<Integer> deleteByValue(Long planId, String value) {
        // Borramos tanto si coincide MCC como MERCHANT_ID:
        return db.sql("""
            DELETE FROM COMMERCE_PLAN_ITEM
             WHERE PLAN_ID=:pid AND (MCC=:v OR MERCHANT_ID=:v)
        """).bind("pid", planId).bind("v", value).fetch().rowsUpdated();
    }

    public Flux<String> listValues(Long planId, int page, int size) {
        int off = Math.max(0,page)*Math.max(1,size);
        // Devolvemos el valor visible (MCC primero, si no Merchant):
        return db.sql("""
            SELECT COALESCE(MCC, MERCHANT_ID) AS VAL
              FROM COMMERCE_PLAN_ITEM
             WHERE PLAN_ID=:pid
             ORDER BY VAL
             OFFSET :off ROWS FETCH NEXT :sz ROWS ONLY
        """).bind("pid", planId).bind("off", off).bind("sz", Math.max(1,size))
                .map((r,m) -> r.get("VAL", String.class)).all();
    }
}

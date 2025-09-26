package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.plan;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
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
public class R2dbcCommercePlanItemRepository implements CommercePlanItemRepository {
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final DatabaseClient db;
    public R2dbcCommercePlanItemRepository(DatabaseClient db) { this.db = db; }

    private static OffsetDateTime toOffset(Row r, String col) {
        LocalDateTime ldt = r.get(col, LocalDateTime.class);
        return ldt != null ? ldt.atZone(ZONE).toOffsetDateTime() : r.get(col, OffsetDateTime.class);
    }

    private static final BiFunction<Row, RowMetadata, PlanItem> MAPPER = (r,m) ->
            PlanItem.rehydrate(
                    r.get("plan_item_id", Long.class),
                    r.get("plan_id", Long.class),
                    r.get("val", String.class),
                    toOffset(r,"created_at"),
                    toOffset(r,"updated_at"),
                    r.get("updated_by", String.class),
                    r.get("status", String.class)
            );

    public Mono<PlanItem> findByValue(Long planId, String value) {
        return db.sql("""
        SELECT PLAN_ITEM_ID, PLAN_ID, COALESCE(MCC, MERCHANT_ID) AS VAL,
               CREATED_AT, UPDATED_AT, UPDATED_BY, STATUS
          FROM COMMERCE_PLAN_ITEM
         WHERE PLAN_ID=:pid AND (MCC=:v OR MERCHANT_ID=:v)
         ORDER BY PLAN_ITEM_ID DESC FETCH FIRST 1 ROWS ONLY
    """).bind("pid", planId).bind("v", value).map(MAPPER).one();
    }

    @Override
    public Mono<PlanItem> insertMcc(Long planId, String mcc, String by) {
        return db.sql("""
            INSERT INTO COMMERCE_PLAN_ITEM (PLAN_ITEM_ID, PLAN_ID, MCC, MERCHANT_ID, CREATED_AT, UPDATED_AT, UPDATED_BY)
            VALUES (SEQ_COMMERCE_PLAN_ITEM_ID.NEXTVAL, :pid, :mcc, NULL, SYSTIMESTAMP, SYSTIMESTAMP, :by)
        """).bind("pid", planId).bind("mcc", mcc).bind("by", by)
                .fetch().rowsUpdated()
                // Rehidratar el registro insertado (el más reciente para ese planId + mcc)
                .then(db.sql("""
              SELECT PLAN_ITEM_ID, PLAN_ID, COALESCE(MCC, MERCHANT_ID) AS VAL, CREATED_AT, UPDATED_AT, UPDATED_BY,STATUS
                FROM COMMERCE_PLAN_ITEM
               WHERE PLAN_ID=:pid AND MCC=:mcc
               ORDER BY PLAN_ITEM_ID DESC
               FETCH FIRST 1 ROWS ONLY
          """).bind("pid", planId).bind("mcc", mcc).map(MAPPER).one());
    }

    public Mono<PlanItem> insertMerchant(Long planId, String merchantId, String by) {
        return db.sql("""
        INSERT INTO COMMERCE_PLAN_ITEM (PLAN_ITEM_ID, PLAN_ID, MCC, MERCHANT_ID, CREATED_AT, UPDATED_AT, UPDATED_BY)
        VALUES (SEQ_COMMERCE_PLAN_ITEM_ID.NEXTVAL, :pid, NULL, :mid, SYSTIMESTAMP, SYSTIMESTAMP, :by)
    """).bind("pid", planId).bind("mid", merchantId).bind("by", by)
                .fetch().rowsUpdated()
                .then(db.sql("""
          SELECT PLAN_ITEM_ID, PLAN_ID, COALESCE(MCC, MERCHANT_ID) AS VAL,
                 CREATED_AT, UPDATED_AT, UPDATED_BY, STATUS
            FROM COMMERCE_PLAN_ITEM
           WHERE PLAN_ID=:pid AND MERCHANT_ID=:mid
           ORDER BY PLAN_ITEM_ID DESC
           FETCH FIRST 1 ROWS ONLY
      """).bind("pid", planId).bind("mid", merchantId).map(MAPPER).one());
    }

    @Override
    public Mono<PlanItem> changeStatus(Long planId, String value, String newStatus, String updatedBy) {
        return db.sql("""
        UPDATE COMMERCE_PLAN_ITEM
           SET STATUS=:st,
               UPDATED_AT=SYSTIMESTAMP,
               UPDATED_BY=:by
         WHERE PLAN_ID=:pid
           AND (MCC=:v OR MERCHANT_ID=:v)
    """).bind("st", newStatus).bind("by", updatedBy)
                .bind("pid", planId).bind("v", value)
                .fetch().rowsUpdated()
                .flatMap(rows -> rows != null && rows > 0
                        ? db.sql("""
                SELECT PLAN_ITEM_ID, PLAN_ID, COALESCE(MCC, MERCHANT_ID) AS VAL,
                       CREATED_AT, UPDATED_AT, UPDATED_BY, STATUS
                  FROM COMMERCE_PLAN_ITEM
                 WHERE PLAN_ID=:pid AND (MCC=:v OR MERCHANT_ID=:v)
                 ORDER BY PLAN_ITEM_ID DESC
                 FETCH FIRST 1 ROWS ONLY
            """).bind("pid", planId).bind("v", value).map(MAPPER).one()
                        : Mono.empty());
    }


    @Override
    public Flux<PlanItem> listItems(Long planId, String status, int page, int size) {
        int off = Math.max(0,page)*Math.max(1,size);
        var sql = new StringBuilder("""
        SELECT PLAN_ITEM_ID, PLAN_ID, COALESCE(MCC, MERCHANT_ID) AS VAL,
               CREATED_AT, UPDATED_AT, UPDATED_BY, STATUS
          FROM COMMERCE_PLAN_ITEM
         WHERE PLAN_ID=:pid
    """);
        if (status != null) sql.append(" AND STATUS=:st");
        sql.append(" ORDER BY VAL OFFSET :off ROWS FETCH NEXT :sz ROWS ONLY");

        var spec = db.sql(sql.toString())
                .bind("pid", planId)
                .bind("off", off).bind("sz", Math.max(1,size));
        if (status != null) spec = spec.bind("st", status);

        return spec.map(MAPPER).all();
    }
}

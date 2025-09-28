package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.plan;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
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

import java.util.List;


@Slf4j
@Repository
public class R2dbcCommercePlanItemRepository implements CommercePlanItemRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final DatabaseClient db;

    public R2dbcCommercePlanItemRepository(DatabaseClient db) {
        this.db = db;
    }

    private static long ms(long t0) {
        return (System.nanoTime() - t0) / 1_000_000;
    }

    private static OffsetDateTime toOffset(Row r, String col) {
        LocalDateTime ldt = r.get(col, LocalDateTime.class);
        return ldt != null ? ldt.atZone(ZONE).toOffsetDateTime() : r.get(col, OffsetDateTime.class);
    }

    private static final BiFunction<Row, RowMetadata, PlanItem> MAPPER = (r, m) ->
            PlanItem.rehydrate(
                    r.get("plan_item_id", Long.class),
                    r.get("plan_id", Long.class),
                    r.get("val", String.class),
                    toOffset(r, "created_at"),
                    toOffset(r, "updated_at"),
                    r.get("updated_by", String.class),
                    r.get("status", String.class)
            );

    @Override
    public Mono<PlanItem> findByValue(Long planId, String value) {
        long t0 = System.nanoTime();
        log.debug("Repo:PLAN_ITEM:findByValue:start planId={} value={}", planId, value);
        return db.sql("""
                            SELECT PLAN_ITEM_ID, PLAN_ID, COALESCE(MCC, MERCHANT_ID) AS VAL,
                                   CREATED_AT, UPDATED_AT, UPDATED_BY, STATUS
                              FROM COMMERCE_PLAN_ITEM
                             WHERE PLAN_ID=:pid AND (MCC=:v OR MERCHANT_ID=:v)
                             ORDER BY PLAN_ITEM_ID DESC FETCH FIRST 1 ROWS ONLY
                        """).bind("pid", planId).bind("v", value)
                .map(MAPPER).one()
                .doOnSuccess(pi -> log.debug("Repo:PLAN_ITEM:findByValue:hit planId={} value={} elapsedMs={}",
                        planId, value, ms(t0)))
                .doOnError(e -> log.warn("Repo:PLAN_ITEM:findByValue:error planId={} value={}", planId, value, e));
    }

    @Override
    public Mono<PlanItem> insertMcc(Long planId, String mcc, String by) {
        long t0 = System.nanoTime();
        log.debug("Repo:PLAN_ITEM:insertMcc:start planId={} mcc={}", planId, mcc);

        var spec = db.sql("""
                    INSERT INTO COMMERCE_PLAN_ITEM (PLAN_ITEM_ID, PLAN_ID, MCC, MERCHANT_ID, CREATED_AT, UPDATED_AT, UPDATED_BY)
                    VALUES (SEQ_COMMERCE_PLAN_ITEM_ID.NEXTVAL, :pid, :mcc, NULL, SYSTIMESTAMP, SYSTIMESTAMP, :by)
                """).bind("pid", planId).bind("mcc", mcc);
        spec = (by == null) ? spec.bindNull("by", String.class) : spec.bind("by", by);

        return spec.fetch().rowsUpdated()
                .doOnNext(n -> log.debug("Repo:PLAN_ITEM:insertMcc:rowsUpdated planId={} n={}", planId, n))
                .then(db.sql("""
                            SELECT PLAN_ITEM_ID, PLAN_ID, COALESCE(MCC, MERCHANT_ID) AS VAL, CREATED_AT, UPDATED_AT, UPDATED_BY, STATUS
                              FROM COMMERCE_PLAN_ITEM
                             WHERE PLAN_ID=:pid AND MCC=:mcc
                             ORDER BY PLAN_ITEM_ID DESC
                             FETCH FIRST 1 ROWS ONLY
                        """).bind("pid", planId).bind("mcc", mcc).map(MAPPER).one())
                .doOnSuccess(pi -> log.info("Repo:PLAN_ITEM:insertMcc:done planId={} itemId={} elapsedMs={}",
                        planId, pi.planItemId(), ms(t0)))
                .doOnError(e -> log.warn("Repo:PLAN_ITEM:insertMcc:error planId={} mcc={}", planId, mcc, e));
    }

    @Override
    public Mono<PlanItem> insertMerchant(Long planId, String merchantId, String by) {
        long t0 = System.nanoTime();
        log.debug("Repo:PLAN_ITEM:insertMerchant:start planId={} merchantId={}", planId, merchantId);

        var spec = db.sql("""
                    INSERT INTO COMMERCE_PLAN_ITEM (PLAN_ITEM_ID, PLAN_ID, MCC, MERCHANT_ID, CREATED_AT, UPDATED_AT, UPDATED_BY)
                    VALUES (SEQ_COMMERCE_PLAN_ITEM_ID.NEXTVAL, :pid, NULL, :mid, SYSTIMESTAMP, SYSTIMESTAMP, :by)
                """).bind("pid", planId).bind("mid", merchantId);
        spec = (by == null) ? spec.bindNull("by", String.class) : spec.bind("by", by);

        return spec.fetch().rowsUpdated()
                .doOnNext(n -> log.debug("Repo:PLAN_ITEM:insertMerchant:rowsUpdated planId={} n={}", planId, n))
                .then(db.sql("""
                            SELECT PLAN_ITEM_ID, PLAN_ID, COALESCE(MCC, MERCHANT_ID) AS VAL,
                                   CREATED_AT, UPDATED_AT, UPDATED_BY, STATUS
                              FROM COMMERCE_PLAN_ITEM
                             WHERE PLAN_ID=:pid AND MERCHANT_ID=:mid
                             ORDER BY PLAN_ITEM_ID DESC
                             FETCH FIRST 1 ROWS ONLY
                        """).bind("pid", planId).bind("mid", merchantId).map(MAPPER).one())
                .doOnSuccess(pi -> log.info("Repo:PLAN_ITEM:insertMerchant:done planId={} itemId={} elapsedMs={}",
                        planId, pi.planItemId(), ms(t0)))
                .doOnError(e -> log.warn("Repo:PLAN_ITEM:insertMerchant:error planId={} mid={}", planId, merchantId, e));
    }

    @Override
    public Mono<PlanItem> changeStatus(Long planId, String value, String newStatus, String updatedBy) {
        long t0 = System.nanoTime();
        log.debug("Repo:PLAN_ITEM:changeStatus:start planId={} value={} status={}", planId, value, newStatus);

        var spec = db.sql("""
                    UPDATE COMMERCE_PLAN_ITEM
                       SET STATUS=:st,
                           UPDATED_AT=SYSTIMESTAMP,
                           UPDATED_BY=:by
                     WHERE PLAN_ID=:pid
                       AND (MCC=:v OR MERCHANT_ID=:v)
                """).bind("st", newStatus).bind("pid", planId).bind("v", value);
        spec = (updatedBy == null) ? spec.bindNull("by", String.class) : spec.bind("by", updatedBy);

        return spec.fetch().rowsUpdated()
                .doOnNext(n -> log.debug("Repo:PLAN_ITEM:changeStatus:rowsUpdated planId={} n={}", planId, n))
                .flatMap(rows -> rows != null && rows > 0
                        ? db.sql("""
                            SELECT PLAN_ITEM_ID, PLAN_ID, COALESCE(MCC, MERCHANT_ID) AS VAL,
                                   CREATED_AT, UPDATED_AT, UPDATED_BY, STATUS
                              FROM COMMERCE_PLAN_ITEM
                             WHERE PLAN_ID=:pid AND (MCC=:v OR MERCHANT_ID=:v)
                             ORDER BY PLAN_ITEM_ID DESC
                             FETCH FIRST 1 ROWS ONLY
                        """).bind("pid", planId).bind("v", value).map(MAPPER).one()
                        : Mono.empty())
                .doOnSuccess(pi -> {
                    if (pi != null) log.info("Repo:PLAN_ITEM:changeStatus:done planId={} itemId={} elapsedMs={}",
                            planId, pi.planItemId(), ms(t0));
                })
                .doOnError(e -> log.warn("Repo:PLAN_ITEM:changeStatus:error planId={} value={} status={}",
                        planId, value, newStatus, e));
    }

    @Override
    public Flux<PlanItem> listItems(Long planId, String status, int page, int size) {
        long t0 = System.nanoTime();
        int p = Math.max(0, page);
        int s = Math.max(1, size);
        int off = p * s;
        log.info("Repo:PLAN_ITEM:listItems:start planId={} status={} page={} size={} offset={}", planId, status, p, s, off);

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
                .bind("off", off)
                .bind("sz", s);
        if (status != null) spec = spec.bind("st", status);

        return spec.map(MAPPER).all()
                .doOnComplete(() -> log.info("Repo:PLAN_ITEM:listItems:done planId={} page={} size={} elapsedMs={}",
                        planId, p, s, ms(t0)))
                .doOnError(e -> log.warn("Repo:PLAN_ITEM:listItems:error planId={} page={} size={}",
                        planId, p, s, e));
    }

    @Override
    public Flux<String> findExistingValues(Long planId, List<String> values) {
        if (values == null || values.isEmpty()) return Flux.empty();

        long t0 = System.nanoTime();
        log.debug("Repo:PLAN_ITEM:findExistingValues:start planId={} size={}", planId, values.size());

        StringBuilder inMcc = new StringBuilder();
        StringBuilder inMid = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) { inMcc.append(','); inMid.append(','); }
            inMcc.append(":m").append(i);
            inMid.append(":n").append(i);
        }

        String sql = ("""
        SELECT COALESCE(MCC, MERCHANT_ID) AS VAL
          FROM COMMERCE_PLAN_ITEM
         WHERE PLAN_ID=:pid
           AND (
                 (MCC IS NOT NULL AND MCC IN (%s))
              OR (MERCHANT_ID IS NOT NULL AND MERCHANT_ID IN (%s))
           )
    """).formatted(inMcc, inMid);

        var spec = db.sql(sql).bind("pid", planId);
        for (int i = 0; i < values.size(); i++) {
            spec = spec.bind("m" + i, values.get(i));
            spec = spec.bind("n" + i, values.get(i));
        }

        return spec.map((r, m) -> r.get("val", String.class)).all()
                .doOnComplete(() -> log.debug("Repo:PLAN_ITEM:findExistingValues:done planId={} elapsedMs={}",
                        planId, (System.nanoTime() - t0) / 1_000_000))
                .doOnError(e -> log.warn("Repo:PLAN_ITEM:findExistingValues:error planId={} size={}", planId, values.size(), e));
    }


    @Override
    public Mono<Integer> insertMccBulk(Long planId, List<String> mccs, String by) {
        if (mccs == null || mccs.isEmpty()) return Mono.just(0);

        long t0 = System.nanoTime();
        log.debug("Repo:PLAN_ITEM:insertMccBulk:start planId={} size={}", planId, mccs.size());

        StringBuilder sb = new StringBuilder("""
        MERGE INTO COMMERCE_PLAN_ITEM t
        USING (
          SELECT :pid AS plan_id, :upd_by AS upd_by, :m0 AS mcc FROM DUAL
    """);
        for (int i = 1; i < mccs.size(); i++) {
            sb.append("\n  UNION ALL SELECT :pid, :upd_by, :m").append(i).append(" FROM DUAL");
        }
        sb.append("""
        ) s
        ON (t.PLAN_ID = s.plan_id AND t.MCC = s.mcc)
        WHEN NOT MATCHED THEN INSERT
          (PLAN_ID, MCC, MERCHANT_ID, CREATED_AT, UPDATED_AT, UPDATED_BY)
        VALUES
          (s.plan_id, s.mcc, NULL, SYSDATE, SYSDATE, s.upd_by)
    """);

        var spec = db.sql(sb.toString()).bind("pid", planId);
        spec = (by == null) ? spec.bindNull("upd_by", String.class) : spec.bind("upd_by", by);
        for (int i = 0; i < mccs.size(); i++) spec = spec.bind("m" + i, mccs.get(i));

        return spec.fetch().rowsUpdated()
                .map(Long::intValue)
                .doOnNext(n -> log.debug("Repo:PLAN_ITEM:insertMccBulk:rowsUpdated planId={} n={}", planId, n))
                .doOnSuccess(n -> log.info("Repo:PLAN_ITEM:insertMccBulk:done planId={} requested={} elapsedMs={}",
                        planId, mccs.size(), (System.nanoTime() - t0) / 1_000_000))
                .doOnError(e -> log.warn("Repo:PLAN_ITEM:insertMccBulk:error planId={} size={}", planId, mccs.size(), e));
    }



    @Override
    public Mono<Integer> insertMerchantBulk(Long planId, List<String> mids, String by) {
        if (mids == null || mids.isEmpty()) return Mono.just(0);

        long t0 = System.nanoTime();
        log.debug("Repo:PLAN_ITEM:insertMerchantBulk:start planId={} size={}", planId, mids.size());

        StringBuilder sb = new StringBuilder("""
        MERGE INTO COMMERCE_PLAN_ITEM t
        USING (
          SELECT :pid AS plan_id, :upd_by AS upd_by, :x0 AS mid FROM DUAL
    """);
        for (int i = 1; i < mids.size(); i++) {
            sb.append("\n  UNION ALL SELECT :pid, :upd_by, :x").append(i).append(" FROM DUAL");
        }
        sb.append("""
        ) s
        ON (t.PLAN_ID = s.plan_id AND t.MERCHANT_ID = s.mid)
        WHEN NOT MATCHED THEN INSERT
          (PLAN_ID, MCC, MERCHANT_ID, CREATED_AT, UPDATED_AT, UPDATED_BY)
        VALUES
          (s.plan_id, NULL, s.mid, SYSDATE, SYSDATE, s.upd_by)
    """);

        var spec = db.sql(sb.toString()).bind("pid", planId);
        spec = (by == null) ? spec.bindNull("upd_by", String.class) : spec.bind("upd_by", by);
        for (int i = 0; i < mids.size(); i++) spec = spec.bind("x" + i, mids.get(i));

        return spec.fetch().rowsUpdated()
                .map(Long::intValue)
                .doOnNext(n -> log.debug("Repo:PLAN_ITEM:insertMerchantBulk:rowsUpdated planId={} n={}", planId, n))
                .doOnSuccess(n -> log.info("Repo:PLAN_ITEM:insertMerchantBulk:done planId={} requested={} elapsedMs={}",
                        planId, mids.size(), (System.nanoTime() - t0) / 1_000_000))
                .doOnError(e -> log.warn("Repo:PLAN_ITEM:insertMerchantBulk:error planId={} size={}", planId, mids.size(), e));
    }

    @Override
    public Mono<Boolean> existsActiveByPlanId(Long planId) {
        long t0 = System.nanoTime();
        log.debug("Repo:PLAN_ITEM:existsActiveByPlanId:start planId={}", planId);

        return db.sql("""
            SELECT 1
              FROM COMMERCE_PLAN_ITEM
             WHERE PLAN_ID=:pid
               AND STATUS='A'
             FETCH FIRST 1 ROWS ONLY
        """)
                .bind("pid", planId)
                .fetch()
                .first()
                .map(m -> true)
                .defaultIfEmpty(false)
                .doOnSuccess(exists ->
                        log.debug("Repo:PLAN_ITEM:existsActiveByPlanId:done planId={} exists={} elapsedMs={}",
                                planId, exists, ms(t0)))
                .doOnError(e ->
                        log.warn("Repo:PLAN_ITEM:existsActiveByPlanId:error planId={}", planId, e));
    }

}
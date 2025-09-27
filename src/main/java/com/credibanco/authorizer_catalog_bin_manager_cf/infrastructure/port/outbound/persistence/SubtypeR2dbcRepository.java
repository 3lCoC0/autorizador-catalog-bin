package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.persistence;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
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
public class SubtypeR2dbcRepository implements SubtypeRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final DatabaseClient client;

    public SubtypeR2dbcRepository(DatabaseClient client) {
        this.client = client;
    }
    private static long ms(long t0) { return (System.nanoTime() - t0) / 1_000_000; }

    private static OffsetDateTime toOffset(Row row, String col) {
        LocalDateTime ldt = row.get(col, LocalDateTime.class);
        if (ldt != null) return ldt.atZone(ZONE).toOffsetDateTime();
        return row.get(col, OffsetDateTime.class);
    }

    private static final BiFunction<Row, RowMetadata, Subtype> SUBTYPE_MAPPER = (row, md) ->
            Subtype.rehydrate(
                    row.get("subtype_code", String.class),
                    row.get("bin",           String.class),
                    row.get("name",          String.class),
                    row.get("description",   String.class),
                    row.get("status",        String.class),
                    row.get("owner_id_type", String.class),
                    row.get("owner_id_number", String.class),
                    row.get("bin_ext",       String.class),
                    row.get("bin_efectivo",  String.class),
                    row.get("subtype_id",    Long.class),
                    toOffset(row, "created_at"),
                    toOffset(row, "updated_at"),
                    row.get("updated_by",    String.class)
            );

    @Override
    public Mono<Boolean> existsByPk(String bin, String subtypeCode) {
        long t0 = System.nanoTime();
        log.debug("Repo:SUBTYPE:existsByPk:start bin={} code={}", bin, subtypeCode);
        return client.sql("""
                SELECT 1 FROM SUBTYPE
                 WHERE BIN=:bin AND SUBTYPE_CODE=:code
                 FETCH FIRST 1 ROWS ONLY
                """)
                .bind("bin", bin)
                .bind("code", subtypeCode)
                .map((r,m) -> 1)
                .first()
                .map(x -> true)
                .defaultIfEmpty(false)
                .doOnSuccess(exists -> log.debug("Repo:SUBTYPE:existsByPk:done bin={} code={} exists={} elapsedMs={}",
                        bin, subtypeCode, exists, ms(t0)))
                .doOnError(e -> log.warn("Repo:SUBTYPE:existsByPk:error bin={} code={}", bin, subtypeCode, e));
    }

    @Override
    public Mono<Boolean> existsByBinAndExt(String bin, String binExt) {
        if (binExt == null) return Mono.just(false);
        long t0 = System.nanoTime();
        log.debug("Repo:SUBTYPE:existsByBinAndExt:start bin={} ext='{}'", bin, binExt);

        return client.sql("""
                SELECT 1 FROM SUBTYPE
                 WHERE BIN=:bin AND BIN_EXT=:ext
                 FETCH FIRST 1 ROWS ONLY
                """)
                .bind("bin", bin)
                .bind("ext", binExt)
                .map((r,m) -> 1)
                .first()
                .map(x -> true)
                .defaultIfEmpty(false)
                .doOnSuccess(exists -> log.debug("Repo:SUBTYPE:existsByBinAndExt:done bin={} ext='{}' exists={} elapsedMs={}",
                        bin, binExt, exists, ms(t0)))
                .doOnError(e -> log.warn("Repo:SUBTYPE:existsByBinAndExt:error bin={} ext='{}'", bin, binExt, e));
    }

    @Override
    public Mono<Subtype> findByPk(String bin, String subtypeCode) {
        long t0 = System.nanoTime();
        log.debug("Repo:SUBTYPE:findByPk:start bin={} code={}", bin, subtypeCode);

        return client.sql("""
                SELECT SUBTYPE_CODE, BIN, NAME, DESCRIPTION, STATUS,
                       OWNER_ID_TYPE, OWNER_ID_NUMBER, BIN_EXT, BIN_EFECTIVO,
                       SUBTYPE_ID, CREATED_AT, UPDATED_AT, UPDATED_BY
                  FROM SUBTYPE
                 WHERE BIN=:bin AND SUBTYPE_CODE=:code
                """)
                .bind("bin", bin)
                .bind("code", subtypeCode)
                .map(SUBTYPE_MAPPER)
                .one()
                .doOnSuccess(s -> log.debug("Repo:SUBTYPE:findByPk:hit bin={} code={} elapsedMs={}",
                        bin, subtypeCode, ms(t0)))
                .doOnError(e -> log.warn("Repo:SUBTYPE:findByPk:error bin={} code={}", bin, subtypeCode, e));
    }

    @Override
    public Flux<Subtype> findAll(String bin, String code, String status, int page, int size) {
        int p = Math.max(0, page);
        int s = Math.max(1, size);
        int offset = p * s;
        long t0 = System.nanoTime();
        log.info("Repo:SUBTYPE:findAll:start bin={} code={} status={} page={} size={} offset={}",
                bin, code, status, p, s, offset);

        var sb = new StringBuilder("""
                SELECT SUBTYPE_CODE, BIN, NAME, DESCRIPTION, STATUS,
                       OWNER_ID_TYPE, OWNER_ID_NUMBER, BIN_EXT, BIN_EFECTIVO,
                       SUBTYPE_ID, CREATED_AT, UPDATED_AT, UPDATED_BY
                  FROM SUBTYPE WHERE 1=1
                """);
        if (bin != null)    sb.append(" AND BIN = :bin");
        if (code != null)   sb.append(" AND SUBTYPE_CODE = :code");
        if (status != null) sb.append(" AND STATUS = :status");
        sb.append(" ORDER BY BIN, SUBTYPE_CODE OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY");

        var spec = client.sql(sb.toString());
        if (bin != null)    spec = spec.bind("bin", bin);
        if (code != null)   spec = spec.bind("code", code);
        if (status != null) spec = spec.bind("status", status);

        return spec.bind("offset", offset)
                .bind("size", s)
                .map(SUBTYPE_MAPPER)
                .all()
                .doOnComplete(() -> log.info("Repo:SUBTYPE:findAll:done bin={} code={} status={} page={} size={} elapsedMs={}",
                        bin, code, status, p, s, ms(t0)))
                .doOnError(e -> log.warn("Repo:SUBTYPE:findAll:error bin={} code={} status={} page={} size={}",
                        bin, code, status, p, s, e));
    }

    @Override
    public Mono<Subtype> save(Subtype e) {
        long t0 = System.nanoTime();
        log.debug("Repo:SUBTYPE:save:start bin={} code={} ext='{}'", e.bin(), e.subtypeCode(), e.binExt());

        var spec = client.sql("""
            MERGE INTO SUBTYPE t
            USING (SELECT :bin BIN, :code SUBTYPE_CODE FROM DUAL) s
               ON (t.BIN = s.BIN AND t.SUBTYPE_CODE = s.SUBTYPE_CODE)
            WHEN MATCHED THEN UPDATE SET
                 t.NAME = :name,
                 t.DESCRIPTION = :description,
                 t.STATUS = :status,
                 t.OWNER_ID_TYPE = :owner_id_type,
                 t.OWNER_ID_NUMBER = :owner_id_number,
                 t.BIN_EXT = :bin_ext,
                 t.UPDATED_BY = :updated_by
            WHEN NOT MATCHED THEN INSERT
                 (SUBTYPE_CODE, BIN, NAME, DESCRIPTION, STATUS,
                  OWNER_ID_TYPE, OWNER_ID_NUMBER, BIN_EXT,
                  CREATED_AT, UPDATED_AT, UPDATED_BY)
            VALUES
                 (:code, :bin, :name, :description, :status,
                  :owner_id_type, :owner_id_number, :bin_ext,
                  SYSTIMESTAMP, SYSTIMESTAMP, :updated_by)
            """)
                .bind("bin", e.bin())
                .bind("code", e.subtypeCode())
                .bind("name", e.name())
                .bind("status", e.status());

        spec = (e.description() != null) ? spec.bind("description", e.description())
                : spec.bindNull("description", String.class);
        spec = (e.ownerIdType() != null) ? spec.bind("owner_id_type", e.ownerIdType())
                : spec.bindNull("owner_id_type", String.class);
        spec = (e.ownerIdNumber() != null) ? spec.bind("owner_id_number", e.ownerIdNumber())
                : spec.bindNull("owner_id_number", String.class);
        spec = (e.binExt() != null) ? spec.bind("bin_ext", e.binExt())
                : spec.bindNull("bin_ext", String.class);
        spec = (e.updatedBy() != null) ? spec.bind("updated_by", e.updatedBy())
                : spec.bindNull("updated_by", String.class);

        return spec.fetch().rowsUpdated()
                .doOnNext(n -> log.debug("Repo:SUBTYPE:save:merge rowsUpdated={} bin={} code={}", n, e.bin(), e.subtypeCode()))
                .then(findByPk(e.bin(), e.subtypeCode()))
                .doOnSuccess(b -> log.info("Repo:SUBTYPE:save:done bin={} code={} elapsedMs={}", b.bin(), b.subtypeCode(), ms(t0)))
                .doOnError(e2 -> log.warn("Repo:SUBTYPE:save:error bin={} code={}", e.bin(), e.subtypeCode(), e2));
    }
}
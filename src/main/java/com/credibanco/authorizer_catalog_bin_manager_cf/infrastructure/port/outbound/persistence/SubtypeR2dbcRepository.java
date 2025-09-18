package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.persistence;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
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
public class SubtypeR2dbcRepository implements SubtypeRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final DatabaseClient client;

    public SubtypeR2dbcRepository(DatabaseClient client) {
        this.client = client;
    }

    /** Igual que en BIN: convierte DATE/TIMESTAMP a OffsetDateTime de forma segura. */
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
                    row.get("descripcion",   String.class),
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
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> existsByBinAndExt(String bin, String binExt) {
        if (binExt == null) return Mono.just(false);
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
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Subtype> findByPk(String bin, String subtypeCode) {
        return client.sql("""
                SELECT SUBTYPE_CODE, BIN, NAME, DESCRIPCION, STATUS,
                       OWNER_ID_TYPE, OWNER_ID_NUMBER, BIN_EXT, BIN_EFECTIVO,
                       SUBTYPE_ID, CREATED_AT, UPDATED_AT, UPDATED_BY
                  FROM SUBTYPE
                 WHERE BIN=:bin AND SUBTYPE_CODE=:code
                """)
                .bind("bin", bin)
                .bind("code", subtypeCode)
                .map(SUBTYPE_MAPPER)
                .one();
    }

    @Override
    public Flux<Subtype> findAll(String bin, String code, String status, int page, int size) {
        int p = Math.max(0, page);
        int s = Math.max(1, size);
        int offset = p * s;

        var sb = new StringBuilder("""
                SELECT SUBTYPE_CODE, BIN, NAME, DESCRIPCION, STATUS,
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
                .all();
    }

    @Override
    public Mono<Subtype> save(Subtype e) {
        var spec = client.sql("""
            MERGE INTO SUBTYPE t
            USING (SELECT :bin BIN, :code SUBTYPE_CODE FROM DUAL) s
               ON (t.BIN = s.BIN AND t.SUBTYPE_CODE = s.SUBTYPE_CODE)
            WHEN MATCHED THEN UPDATE SET
                 t.NAME = :name,
                 t.DESCRIPCION = :descripcion,
                 t.STATUS = :status,
                 t.OWNER_ID_TYPE = :owner_id_type,
                 t.OWNER_ID_NUMBER = :owner_id_number,
                 t.BIN_EXT = :bin_ext,
                 t.UPDATED_BY = :updated_by
            WHEN NOT MATCHED THEN INSERT
                 (SUBTYPE_CODE, BIN, NAME, DESCRIPCION, STATUS,
                  OWNER_ID_TYPE, OWNER_ID_NUMBER, BIN_EXT,
                  CREATED_AT, UPDATED_AT, UPDATED_BY)
            VALUES
                 (:code, :bin, :name, :descripcion, :status,
                  :owner_id_type, :owner_id_number, :bin_ext,
                  SYSTIMESTAMP, SYSTIMESTAMP, :updated_by)
            """)
                .bind("bin", e.bin())
                .bind("code", e.subtypeCode())
                .bind("name", e.name())
                .bind("status", e.status())
                .bind("updated_by", e.updatedBy());

        // Opcionales: bind o bindNull como en BIN
        if (e.descripcion() != null) spec = spec.bind("descripcion", e.descripcion());
        else                         spec = spec.bindNull("descripcion", String.class);

        if (e.ownerIdType() != null) spec = spec.bind("owner_id_type", e.ownerIdType());
        else                         spec = spec.bindNull("owner_id_type", String.class);

        if (e.ownerIdNumber() != null) spec = spec.bind("owner_id_number", e.ownerIdNumber());
        else                           spec = spec.bindNull("owner_id_number", String.class);

        if (e.binExt() != null) spec = spec.bind("bin_ext", e.binExt());
        else                    spec = spec.bindNull("bin_ext", String.class);

        return spec.fetch().rowsUpdated()
                .then(findByPk(e.bin(), e.subtypeCode()));
    }
}
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.persistence;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
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
public class R2dbcBinRepository implements BinRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final DatabaseClient client;

    public R2dbcBinRepository(DatabaseClient client) {
        this.client = client;
    }

    private static OffsetDateTime toOffset(Row row, String col) {
        // 1) Intenta como LocalDateTime (DATE/TIMESTAMP sin TZ)
        LocalDateTime ldt = row.get(col, LocalDateTime.class);
        if (ldt != null) return ldt.atZone(ZONE).toOffsetDateTime();
        // 2) Si la columna es TIMESTAMP WITH TIME ZONE, el driver puede devolver OffsetDateTime
        return row.get(col, OffsetDateTime.class);
    }

    private static final BiFunction<Row, RowMetadata, Bin> BIN_MAPPER = (row, md) ->
            Bin.rehydrate(
                    row.get("bin", String.class),
                    row.get("name", String.class),
                    row.get("type_bin", String.class),
                    row.get("type_account", String.class),
                    row.get("compensation_cod", String.class),
                    row.get("description", String.class),
                    row.get("status", String.class),
                    toOffset(row, "created_at"),
                    toOffset(row, "updated_at"),
                    row.get("updated_by", String.class),
                    row.get("uses_bin_ext", String.class),
                    row.get("bin_ext_digits", Integer.class)
            );

    @Override
    public Mono<Boolean> existsById(String bin) {
        return client.sql("SELECT 1 FROM BIN WHERE BIN = :bin")
                .bind("bin", bin)
                .map((r, m) -> 1)
                .first()
                .map(x -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Bin> save(Bin bin) {
        var spec = client.sql("""
                            MERGE INTO BIN t
                            USING (SELECT :bin BIN FROM DUAL) s
                               ON (t.BIN = s.BIN)
                            WHEN MATCHED THEN UPDATE SET
                                t.NAME = :name,
                                t.TYPE_BIN = :type_bin,
                                t.TYPE_ACCOUNT = :type_account,
                                t.COMPENSATION_COD = :compensation_cod,
                                t.DESCRIPTION = :description,
                                t.STATUS = :status,
                                t.UPDATED_BY = :updated_by,
                                t.USES_BIN_EXT = :uses_bin_ext,
                                t.BIN_EXT_DIGITS = :bin_ext_digits
                            WHEN NOT MATCHED THEN INSERT
                                (BIN, NAME, TYPE_BIN, TYPE_ACCOUNT, COMPENSATION_COD, DESCRIPTION, STATUS,
                                 CREATED_AT, UPDATED_AT, UPDATED_BY, USES_BIN_EXT, BIN_EXT_DIGITS)
                            VALUES
                                (:bin, :name, :type_bin, :type_account, :compensation_cod, :description, :status,
                                 SYSTIMESTAMP, SYSTIMESTAMP, :updated_by, :uses_bin_ext, :bin_ext_digits)
                        """)
                .bind("bin", bin.bin())
                .bind("name", bin.name())
                .bind("type_bin", bin.typeBin())
                .bind("type_account", bin.typeAccount())
                .bind("status", bin.status());

        spec = (bin.compensationCod() != null)
                ? spec.bind("compensation_cod", bin.compensationCod())
                : spec.bindNull("compensation_cod", String.class);
        spec = (bin.description() != null)
                ? spec.bind("description", bin.description())
                : spec.bindNull("description", String.class);
        spec = (bin.updatedBy() != null)
                ? spec.bind("updated_by", bin.updatedBy())
                : spec.bindNull("updated_by", String.class);
        spec = spec.bind("uses_bin_ext", bin.usesBinExt());
        spec = (bin.binExtDigits() != null)
                ? spec.bind("bin_ext_digits", bin.binExtDigits())
                : spec.bindNull("bin_ext_digits", Integer.class);

        return spec.fetch().rowsUpdated().then(findById(bin.bin()));
    }

    @Override
    public Mono<Bin> findById(String bin) {
        return client.sql("""
                SELECT BIN, NAME, TYPE_BIN, TYPE_ACCOUNT, COMPENSATION_COD, DESCRIPTION,
                     STATUS, CREATED_AT, UPDATED_AT, UPDATED_BY,
                     USES_BIN_EXT, BIN_EXT_DIGITS
                     FROM BIN
                     WHERE BIN = :bin;
                """)
                .bind("bin", bin)
                .map(BIN_MAPPER)
                .one();
    }

    @Override
    public Flux<Bin> findAll(int page, int size) {
        int p = Math.max(0, page);
        int s = Math.max(1, size);
        int offset = p * s;

        return client.sql("""
                SELECT BIN, NAME, TYPE_BIN, TYPE_ACCOUNT, COMPENSATION_COD, DESCRIPTION,
                       STATUS, CREATED_AT, UPDATED_AT, UPDATED_BY,
                       USES_BIN_EXT, BIN_EXT_DIGITS
                  FROM BIN
                 ORDER BY BIN ASC
                 OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
                """)
                .bind("offset", offset)
                .bind("size", s)
                .map(BIN_MAPPER)
                .all();
    }
}

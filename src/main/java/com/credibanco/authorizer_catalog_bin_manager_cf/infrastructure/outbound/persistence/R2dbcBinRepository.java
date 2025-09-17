package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.outbound.persistence;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.BinRepository;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.function.BiFunction;

@Repository
public class R2dbcBinRepository implements BinRepository {

    private final DatabaseClient client;

    public R2dbcBinRepository(DatabaseClient client) {
        this.client = client;
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
                    row.get("created_at", OffsetDateTime.class),
                    row.get("updated_at", OffsetDateTime.class),
                    row.get("updated_by", String.class)
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
        // Upsert con MERGE (timestamps los maneja tu trigger en DB)
        return client.sql("""
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
                    t.UPDATED_BY = :updated_by
                WHEN NOT MATCHED THEN INSERT
                    (BIN, NAME, TYPE_BIN, TYPE_ACCOUNT, COMPENSATION_COD, DESCRIPTION, STATUS, CREATED_AT, UPDATED_AT, UPDATED_BY)
                VALUES
                    (:bin, :name, :type_bin, :type_account, :compensation_cod, :description, :status, SYSTIMESTAMP, SYSTIMESTAMP, :updated_by)
                """)
                .bind("bin", bin.bin())
                .bind("name", bin.name())
                .bind("type_bin", bin.typeBin())
                .bind("type_account", bin.typeAccount())
                .bind("compensation_cod", bin.compensationCod())
                .bind("description", bin.description())
                .bind("status", bin.status())
                .bind("updated_by", bin.updatedBy())
                .fetch().rowsUpdated()
                .then(findById(bin.bin())); // leemos el registro final desde DB
    }

    @Override
    public Mono<Bin> findById(String bin) {
        return client.sql("""
                SELECT BIN, NAME, TYPE_BIN, TYPE_ACCOUNT, COMPENSATION_COD, DESCRIPTION,
                       STATUS, CREATED_AT, UPDATED_AT, UPDATED_BY
                  FROM BIN
                 WHERE BIN = :bin
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
                       STATUS, CREATED_AT, UPDATED_AT, UPDATED_BY
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

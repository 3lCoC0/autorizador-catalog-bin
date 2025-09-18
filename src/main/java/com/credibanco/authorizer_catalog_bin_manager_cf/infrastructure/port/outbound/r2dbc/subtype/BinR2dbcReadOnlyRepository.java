package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.subtype;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class BinR2dbcReadOnlyRepository implements BinReadOnlyRepository {
    private final DatabaseClient db;
    public BinR2dbcReadOnlyRepository(DatabaseClient db) { this.db = db; }

    @Override
    public Mono<Boolean> existsById(String bin) {
        return db.sql("SELECT 1 FROM BIN WHERE BIN = :bin FETCH FIRST 1 ROWS ONLY")
                .bind("bin", bin)
                .fetch()
                .first()
                .map(m -> true)
                .defaultIfEmpty(false);
    }
}

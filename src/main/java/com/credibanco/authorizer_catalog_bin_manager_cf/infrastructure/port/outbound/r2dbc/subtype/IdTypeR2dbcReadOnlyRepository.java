package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.subtype;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.IdTypeReadOnlyRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class IdTypeR2dbcReadOnlyRepository implements IdTypeReadOnlyRepository {
    private final DatabaseClient db;
    public IdTypeR2dbcReadOnlyRepository(DatabaseClient db) { this.db = db; }

    @Override
    public Mono<Boolean> existsById(String idType) {
        return db.sql("SELECT 1 FROM ID_TYPE WHERE ID_TYPE_CODE = :id FETCH FIRST 1 ROWS ONLY")
                .bind("id", idType)
                .fetch()
                .first()
                .map(m -> true)
                .defaultIfEmpty(false);
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.agency;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class SubtypeR2dbcReadOnlyRepository implements SubtypeReadOnlyRepository {
    private final DatabaseClient db;
    public SubtypeR2dbcReadOnlyRepository(DatabaseClient db) { this.db = db; }

    @Override
    public Mono<Boolean> isActive(String subtypeCode) {
        return db.sql("""
                SELECT 1 FROM SUBTYPE
                 WHERE SUBTYPE_CODE=:code AND STATUS='A'
                 FETCH FIRST 1 ROWS ONLY
                """)
                .bind("code", subtypeCode)
                .fetch().first()
                .map(m -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> existsByCode(String subtypeCode) { // NUEVO
        return db.sql("""
                SELECT 1 FROM SUBTYPE
                 WHERE SUBTYPE_CODE=:code
                 FETCH FIRST 1 ROWS ONLY
                """)
                .bind("code", subtypeCode)
                .fetch().first()
                .map(m -> true)
                .defaultIfEmpty(false);
    }
}

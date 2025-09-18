package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.subtype;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.AgencyReadOnlyRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class AgencyR2dbcReadOnlyRepository implements AgencyReadOnlyRepository {
    private final DatabaseClient db;
    public AgencyR2dbcReadOnlyRepository(DatabaseClient db) { this.db = db; }

    @Override
    public Mono<Long> countActiveBySubtypeCode(String subtypeCode) {
        return db.sql("""
                SELECT COUNT(*) c
                  FROM AGENCY
                 WHERE SUBTYPE_CODE = :code
                   AND STATUS = 'A'
                """)
                .bind("code", subtypeCode)
                .map((r, m) -> {
                    Long v = r.get("C", Long.class);
                    if (v != null) return v;
                    Integer i = r.get("C", Integer.class);
                    return i == null ? 0L : i.longValue();
                })
                .one();
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.agency;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class SubtypeR2dbcReadOnlyRepository implements
        com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.SubtypeReadOnlyRepository,
        com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.SubtypeReadOnlyRepository,
        com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypeReadOnlyRepository {

    private final DatabaseClient db;
    public SubtypeR2dbcReadOnlyRepository(DatabaseClient db) { this.db = db; }

    @Override
    public Mono<Boolean> isActive(String subtypeCode) {
        return db.sql("""
            SELECT 1 FROM SUBTYPE
             WHERE SUBTYPE_CODE=:code AND STATUS='A'
             FETCH FIRST 1 ROWS ONLY
        """).bind("code", subtypeCode).fetch().first().map(m->true).defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> existsByCode(String subtypeCode) {
        return db.sql("""
            SELECT 1 FROM SUBTYPE
             WHERE SUBTYPE_CODE=:code
             FETCH FIRST 1 ROWS ONLY
        """).bind("code", subtypeCode).fetch().first().map(m->true).defaultIfEmpty(false);
    }


    @Override
    public Mono<Boolean> existsByCodeAndBinEfectivo(String code, String eff) {
        return db.sql("""
            SELECT 1 FROM SUBTYPE
             WHERE SUBTYPE_CODE=:code AND BIN=:eff
             FETCH FIRST 1 ROWS ONLY
        """).bind("code", code).bind("eff", eff).fetch().first().map(m->true).defaultIfEmpty(false);
    }
}


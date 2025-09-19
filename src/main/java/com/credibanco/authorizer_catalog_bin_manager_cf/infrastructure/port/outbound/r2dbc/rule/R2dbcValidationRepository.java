package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.r2dbc.rule;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationDataType;
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
public class R2dbcValidationRepository implements ValidationRepository {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final DatabaseClient db;
    public R2dbcValidationRepository(DatabaseClient db) { this.db = db; }

    private static OffsetDateTime toOffset(Row r, String col) {
        LocalDateTime ldt = r.get(col, LocalDateTime.class);
        return ldt != null ? ldt.atZone(ZONE).toOffsetDateTime() : r.get(col, OffsetDateTime.class);
    }

    private static final BiFunction<Row, RowMetadata, Validation> MAPPER = (r,m) ->
            Validation.rehydrate(
                    r.get("validation_id", Long.class),
                    r.get("code", String.class),
                    r.get("description", String.class),
                    ValidationDataType.valueOf(r.get("data_type", String.class)),
                    r.get("value_flag", String.class),
                    r.get("value_num", Double.class),
                    r.get("value_text", String.class),
                    r.get("status", String.class),
                    toOffset(r,"valid_from"),
                    toOffset(r,"valid_to"),
                    toOffset(r,"created_at"),
                    toOffset(r,"updated_at")
            );

    @Override public Mono<Boolean> existsByCode(String code) {
        return db.sql("SELECT 1 FROM SUBTYPE_VALIDATION WHERE CODE=:c FETCH FIRST 1 ROWS ONLY")
                .bind("c", code).fetch().first().map(m->true).defaultIfEmpty(false);
    }

    @Override public Mono<Validation> save(Validation v) {
        var spec = db.sql("""
            MERGE INTO SUBTYPE_VALIDATION t
            USING (SELECT :code CODE FROM DUAL) s
               ON (t.CODE = s.CODE)
            WHEN MATCHED THEN UPDATE SET
                 t.DESCRIPTION=:descr, t.DATA_TYPE=:type,
                 t.VALUE_FLAG=:f, t.VALUE_NUM=:n, t.VALUE_TEXT=:t,
                 t.STATUS=:status, t.VALID_FROM=:vf, t.VALID_TO=:vt, t.UPDATED_AT=SYSTIMESTAMP
            WHEN NOT MATCHED THEN INSERT
                 (VALIDATION_ID, CODE, DESCRIPTION, DATA_TYPE, VALUE_FLAG, VALUE_NUM, VALUE_TEXT,
                  STATUS, VALID_FROM, VALID_TO, CREATED_AT, UPDATED_AT)
            VALUES (SEQ_SUBTIPO_VALIDATION_ID.NEXTVAL, :code, :descr, :type, :f, :n, :t,
                    :status, :vf, :vt, SYSTIMESTAMP, SYSTIMESTAMP)
            """)
                .bind("code", v.code())
                .bind("descr", v.description())
                .bind("type", v.dataType().name())
                .bind("status", v.status());

        spec = (v.valueFlag()!=null) ? spec.bind("f", v.valueFlag().toUpperCase()) : spec.bindNull("f", String.class);
        spec = (v.valueNum()!=null)  ? spec.bind("n", v.valueNum())               : spec.bindNull("n", Double.class);
        spec = (v.valueText()!=null) ? spec.bind("t", v.valueText())              : spec.bindNull("t", String.class);
        spec = (v.validFrom()!=null) ? spec.bind("vf", v.validFrom())             : spec.bindNull("vf", OffsetDateTime.class);
        spec = (v.validTo()!=null)   ? spec.bind("vt", v.validTo())               : spec.bindNull("vt", OffsetDateTime.class);

        return spec.fetch().rowsUpdated().then(findByCode(v.code()));
    }

    @Override public Mono<Validation> findByCode(String code) {
        return db.sql("""
                SELECT VALIDATION_ID, CODE, DESCRIPTION, DATA_TYPE, VALUE_FLAG, VALUE_NUM, VALUE_TEXT,
                       STATUS, VALID_FROM, VALID_TO, CREATED_AT, UPDATED_AT
                  FROM SUBTYPE_VALIDATION WHERE CODE=:c
                """).bind("c", code).map(MAPPER).one();
    }

    @Override public Mono<Validation> findById(Long id) {
        return db.sql("""
                SELECT VALIDATION_ID, CODE, DESCRIPTION, DATA_TYPE, VALUE_FLAG, VALUE_NUM, VALUE_TEXT,
                       STATUS, VALID_FROM, VALID_TO, CREATED_AT, UPDATED_AT
                  FROM SUBTYPE_VALIDATION WHERE VALIDATION_ID=:id
                """).bind("id", id).map(MAPPER).one();
    }

    @Override public Flux<Validation> findAll(String status, String search, int page, int size) {
        int p=Math.max(0,page), s=Math.max(1,size), off=p*s;
        var sb = new StringBuilder("""
                SELECT VALIDATION_ID, CODE, DESCRIPTION, DATA_TYPE, VALUE_FLAG, VALUE_NUM, VALUE_TEXT,
                       STATUS, VALID_FROM, VALID_TO, CREATED_AT, UPDATED_AT
                  FROM SUBTYPE_VALIDATION WHERE 1=1
                """);
        if (status!=null) sb.append(" AND STATUS=:st");
        if (search!=null) sb.append(" AND (UPPER(CODE) LIKE '%'||UPPER(:q)||'%' OR UPPER(DESCRIPTION) LIKE '%'||UPPER(:q)||'%')");
        sb.append(" ORDER BY CODE OFFSET :off ROWS FETCH NEXT :sz ROWS ONLY");

        var spec = db.sql(sb.toString());
        if (status!=null) spec = spec.bind("st", status);
        if (search!=null) spec = spec.bind("q", search);
        return spec.bind("off", off).bind("sz", s).map(MAPPER).all();
    }
}

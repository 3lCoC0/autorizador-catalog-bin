package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.UpdatePlanUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.Objects;

@Slf4j
public record UpdatePlanService(CommercePlanRepository repo) implements UpdatePlanUseCase {
    @Override
    public Mono<CommercePlan> execute(String planCode, String planName, String description,
                                      String validationMode /* nullable */, String updatedBy) {
        log.info("UpdatePlanService IN code={} newName={} mode={} by={}", planCode, planName, validationMode, updatedBy);

        return repo.findByCode(planCode)
                .switchIfEmpty(Mono.<CommercePlan>error(new AppException(AppError.PLAN_NOT_FOUND)))
                .map(p -> {
                    try {
                        String newMode = validationMode == null ? null : CommerceValidationMode.fromJson(validationMode).name();
                        return p.updateBasics(planName, newMode, description, updatedBy);
                    } catch (IllegalArgumentException iae) {
                        throw new AppException(AppError.PLAN_INVALID_DATA, iae.getMessage());
                    }
                })
                .flatMap(repo::save)
                .onErrorMap(ex -> {
                    String oracleMessage = findOracleBusinessMessage(ex);
                    if (oracleMessage != null) {
                        return new AppException(AppError.PLAN_INVALID_DATA, oracleMessage);
                    }
                    return ex;
                })
                .doOnSuccess(p -> log.info("UpdatePlanService OK code={} id={}", p.code(), p.planId()));
    }

    private static String findOracleBusinessMessage(Throwable error) {
        Throwable cursor = error;
        while (cursor != null) {
            String parsed = parseOracleBusinessMessage(cursor.getMessage());
            if (parsed != null) {
                return parsed;
            }
            if (cursor instanceof SQLException sql) {
                parsed = parseOracleBusinessMessage(sql.getSQLState());
                if (parsed != null) {
                    return parsed;
                }
                parsed = parseOracleBusinessMessage(sql.getMessage());
                if (parsed != null) {
                    return parsed;
                }
            }
            cursor = cursor.getCause();
        }
        return null;
    }

    private static String parseOracleBusinessMessage(String message) {
        if (message == null) {
            return null;
        }
        int marker = message.indexOf("ORA-20601:");
        if (marker < 0) {
            return null;
        }
        String after = message.substring(marker + "ORA-20601:".length()).trim();
        int newline = after.indexOf('\n');
        if (newline >= 0) {
            after = after.substring(0, newline);
        }
        return after.isBlank() ? null : after.trim();
    }
}

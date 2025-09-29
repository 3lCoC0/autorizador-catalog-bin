package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.ListRulesForSubtypeUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import reactor.core.publisher.Flux;

public record ListRulesForSubtypeService(ValidationMapRepository mapRepo)
        implements ListRulesForSubtypeUseCase {
    @Override
    public Flux<ValidationMap> execute(String subtypeCode, String bin, String status, int page, int size) {
        if (page < 0 || size <= 0) {
            return Flux.error(new AppException(AppError.RULES_MAP_INVALID_DATA, "page>=0 y size>0"));
        }
        String s = (status != null && status.isBlank()) ? null : status;
        return mapRepo.findResolved(subtypeCode, bin, s, page, size);
    }
}

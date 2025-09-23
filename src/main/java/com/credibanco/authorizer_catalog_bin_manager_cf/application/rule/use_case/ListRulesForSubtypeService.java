package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound.ListRulesForSubtypeUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import reactor.core.publisher.Flux;

public record ListRulesForSubtypeService(ValidationMapRepository mapRepo)
        implements ListRulesForSubtypeUseCase {
    @Override
    public Flux<ValidationMap> execute(String subtypeCode, String bin, String status, int page, int size) {
        String s = (status != null && status.isBlank()) ? null : status;
        return mapRepo.findResolved(subtypeCode, bin, s, page, size);
    }
}

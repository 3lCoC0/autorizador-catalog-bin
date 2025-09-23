package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import reactor.core.publisher.Mono;


public interface MapRuleUseCase {
    Mono<ValidationMap> attach(String subtypeCode, String bin, String validationCode, Object value, String by);
    Mono<ValidationMap> changeStatus(String subtypeCode, String bin, String validationCode, String newStatus, String by);
}

package com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.inbound;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import reactor.core.publisher.Mono;

public interface MapRuleUseCase {
    Mono<ValidationMap> attach(String subtypeCode, String binEfectivo, String validationCode, int priority, String by);
    Mono<ValidationMap> changeStatus(String subtypeCode, String binEfectivo, String validationCode, String newStatus, String by);
}

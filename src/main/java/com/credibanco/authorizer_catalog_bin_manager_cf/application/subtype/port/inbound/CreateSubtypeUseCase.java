package com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.inbound;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import reactor.core.publisher.Mono;

public interface CreateSubtypeUseCase {
    Mono<Subtype> execute(String subtypeCode, String bin, String name, String descripcion,
                          String ownerIdType, String ownerIdNumber, String binExt, String createdBy);
}
package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.IdTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdTypeJpaRepository extends JpaRepository<IdTypeEntity, String> {
}

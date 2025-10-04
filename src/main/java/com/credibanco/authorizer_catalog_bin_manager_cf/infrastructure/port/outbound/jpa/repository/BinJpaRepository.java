package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.BinEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BinJpaRepository extends JpaRepository<BinEntity, String> {
}

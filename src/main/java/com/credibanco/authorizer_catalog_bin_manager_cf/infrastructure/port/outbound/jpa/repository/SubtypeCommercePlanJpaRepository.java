package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeCommercePlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubtypeCommercePlanJpaRepository extends JpaRepository<SubtypeCommercePlanEntity, Long> {

    Optional<SubtypeCommercePlanEntity> findBySubtypeCode(String subtypeCode);
}

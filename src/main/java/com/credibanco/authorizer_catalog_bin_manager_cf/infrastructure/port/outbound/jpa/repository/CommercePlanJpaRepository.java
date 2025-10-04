package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.CommercePlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CommercePlanJpaRepository extends JpaRepository<CommercePlanEntity, Long>,
        JpaSpecificationExecutor<CommercePlanEntity> {

    boolean existsByPlanCode(String code);

    Optional<CommercePlanEntity> findByPlanCode(String code);
}

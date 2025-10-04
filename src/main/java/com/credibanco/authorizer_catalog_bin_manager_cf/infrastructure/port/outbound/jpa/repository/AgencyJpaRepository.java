package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.AgencyEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.AgencyEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AgencyJpaRepository extends JpaRepository<AgencyEntity, AgencyEntityId>,
        JpaSpecificationExecutor<AgencyEntity> {

    boolean existsByIdSubtypeCodeAndStatusAndIdAgencyCodeNot(String subtypeCode, String status, String agencyCode);

    long countByIdSubtypeCodeAndStatus(String subtypeCode, String status);
}

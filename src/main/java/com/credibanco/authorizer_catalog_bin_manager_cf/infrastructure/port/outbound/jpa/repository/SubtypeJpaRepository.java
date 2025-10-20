package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SubtypeJpaRepository extends JpaRepository<SubtypeEntity, SubtypeEntityId>,
        JpaSpecificationExecutor<SubtypeEntity> {

    boolean existsByIdBinAndBinExt(String bin, String binExt);

    boolean existsByIdSubtypeCode(String subtypeCode);

    boolean existsByIdSubtypeCodeAndStatus(String subtypeCode, String status);

    boolean existsByIdSubtypeCodeAndIdBin(String subtypeCode, String bin);

    boolean existsByIdBin(String bin);

}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.ValidationMapEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ValidationMapJpaRepository extends JpaRepository<ValidationMapEntity, Long>,
        JpaSpecificationExecutor<ValidationMapEntity> {

    boolean existsBySubtypeCodeAndBinAndValidationIdAndStatus(String subtypeCode, String bin, Long validationId, String status);

    Optional<ValidationMapEntity> findBySubtypeCodeAndBinAndValidationId(String subtypeCode, String bin, Long validationId);

    @Query("SELECT m FROM ValidationMapEntity m JOIN m.validation v WHERE m.subtypeCode = :subtypeCode " +
            "AND (:bin IS NULL OR m.bin = :bin) AND (:status IS NULL OR m.status = :status)")
    List<ValidationMapEntity> findResolved(@Param("subtypeCode") String subtypeCode,
                                           @Param("bin") String bin,
                                           @Param("status") String status,
                                           Pageable pageable);
}

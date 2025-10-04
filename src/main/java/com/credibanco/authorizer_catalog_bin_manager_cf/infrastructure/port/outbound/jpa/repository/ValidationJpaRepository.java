package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.ValidationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ValidationJpaRepository extends JpaRepository<ValidationEntity, Long>,
        JpaSpecificationExecutor<ValidationEntity> {

    boolean existsByCode(String code);

    Optional<ValidationEntity> findByCode(String code);

    @Query("SELECT v FROM ValidationEntity v WHERE v.validationId = :id AND v.status = 'A' " +
            "AND v.validFrom <= :now AND (v.validTo IS NULL OR v.validTo >= :now)")
    Optional<ValidationEntity> findActiveById(@Param("id") Long id, @Param("now") OffsetDateTime now);
}

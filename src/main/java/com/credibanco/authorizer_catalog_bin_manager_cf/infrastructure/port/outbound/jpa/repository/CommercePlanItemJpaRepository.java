package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository;

import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.CommercePlanItemEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommercePlanItemJpaRepository extends JpaRepository<CommercePlanItemEntity, Long>,
        JpaSpecificationExecutor<CommercePlanItemEntity> {

    @Query("SELECT i FROM CommercePlanItemEntity i WHERE i.planId = :planId AND (i.mcc = :value OR i.merchantId = :value)")
    List<CommercePlanItemEntity> findByPlanIdAndValue(@Param("planId") Long planId,
                                                       @Param("value") String value,
                                                       Pageable pageable);

    @Query("SELECT CASE WHEN i.mcc IS NOT NULL THEN i.mcc ELSE i.merchantId END FROM CommercePlanItemEntity i " +
            "WHERE i.planId = :planId AND ((i.mcc IS NOT NULL AND i.mcc IN :values) OR " +
            "(i.merchantId IS NOT NULL AND i.merchantId IN :values))")
    List<String> findExistingValues(@Param("planId") Long planId, @Param("values") List<String> values);

    boolean existsByPlanIdAndStatus(Long planId, String status);
}

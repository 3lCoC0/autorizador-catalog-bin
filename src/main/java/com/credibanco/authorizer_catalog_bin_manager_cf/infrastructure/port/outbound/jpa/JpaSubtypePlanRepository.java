package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.SubtypePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.SubtypePlanLink;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeCommercePlanEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.SubtypePlanJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.SubtypeCommercePlanJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.Objects;

@Repository
public class JpaSubtypePlanRepository implements SubtypePlanRepository {

    private final SubtypeCommercePlanJpaRepository repository;
    private final TransactionTemplate txTemplate;

    public JpaSubtypePlanRepository(SubtypeCommercePlanJpaRepository repository,
                                    PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Mono<Integer> upsert(String subtypeCode, Long planId, String updatedBy) {
        return Mono.defer(() -> Mono.fromCallable(() ->
                Objects.requireNonNull(txTemplate.execute(status -> {
                    SubtypeCommercePlanEntity entity = repository.findBySubtypeCode(subtypeCode)
                            .orElseGet(SubtypeCommercePlanEntity::new);
                    boolean isNew = entity.getSubtypePlanId() == null;
                    entity.setSubtypeCode(subtypeCode);
                    entity.setPlanId(planId);
                    OffsetDateTime now = OffsetDateTime.now();
                    if (isNew) {
                        entity.setCreatedAt(now);
                    }
                    entity.setUpdatedAt(now);
                    entity.setUpdatedBy(updatedBy);
                    repository.save(entity);
                    return 1;
                }))
        )).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<SubtypePlanLink> findBySubtype(String subtypeCode) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.findBySubtypeCode(subtypeCode)
                        .map(SubtypePlanJpaMapper::toDomain)
                        .orElseThrow(() -> new NoSuchElementException("SUBTYPE plan not found: " + subtypeCode))))
                .subscribeOn(Schedulers.boundedElastic());
    }
}

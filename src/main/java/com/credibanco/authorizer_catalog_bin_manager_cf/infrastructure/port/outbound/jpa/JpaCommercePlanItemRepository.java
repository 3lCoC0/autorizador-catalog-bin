package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.CommercePlanItemEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.PlanItemJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.CommercePlanItemJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@Repository
public class JpaCommercePlanItemRepository implements CommercePlanItemRepository {

    private final CommercePlanItemJpaRepository repository;
    private final TransactionTemplate txTemplate;

    public JpaCommercePlanItemRepository(CommercePlanItemJpaRepository repository,
                                         PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Mono<PlanItem> insertMcc(Long planId, String mcc, String by) {
        return Mono.defer(() -> Mono.fromCallable(() ->
                Objects.requireNonNull(txTemplate.execute(status -> {
                    OffsetDateTime now = OffsetDateTime.now();
                    CommercePlanItemEntity entity = new CommercePlanItemEntity();
                    entity.setPlanId(planId);
                    entity.setMcc(mcc);
                    entity.setMerchantId(null);
                    entity.setStatus("A");
                    entity.setCreatedAt(now);
                    entity.setUpdatedAt(now);
                    entity.setUpdatedBy(by);
                    CommercePlanItemEntity saved = repository.save(entity);
                    return PlanItemJpaMapper.toDomain(saved);
                }))
        )).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<PlanItem> changeStatus(Long planId, String value, String newStatus, String updatedBy) {
        return Mono.defer(() -> Mono.fromCallable(() ->
                txTemplate.execute(status -> {
                    CommercePlanItemEntity entity = findLatest(planId, value);
                    if (entity == null) {
                        return null;
                    }
                    entity.setStatus(newStatus);
                    entity.setUpdatedAt(OffsetDateTime.now());
                    entity.setUpdatedBy(updatedBy);
                    CommercePlanItemEntity saved = repository.save(entity);
                    return PlanItemJpaMapper.toDomain(saved);
                }))
        ).flatMap(result -> result == null ? Mono.<PlanItem>empty() : Mono.just(result))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<PlanItem> listItems(Long planId, String status, int page, int size) {
        return Flux.defer(() -> {
                    Specification<CommercePlanItemEntity> spec = buildSpecification(planId, status);
                    int p = Math.max(0, page);
                    int s = Math.max(1, size);
                    List<CommercePlanItemEntity> content = repository.findAll(spec,
                                    PageRequest.of(p, s, Sort.by(Sort.Order.asc("mcc"), Sort.Order.asc("merchantId"))))
                            .getContent();
                    return Flux.fromIterable(content).map(PlanItemJpaMapper::toDomain);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<PlanItem> insertMerchant(Long planId, String merchantId, String updatedBy) {
        return Mono.defer(() -> Mono.fromCallable(() ->
                Objects.requireNonNull(txTemplate.execute(status -> {
                    OffsetDateTime now = OffsetDateTime.now();
                    CommercePlanItemEntity entity = new CommercePlanItemEntity();
                    entity.setPlanId(planId);
                    entity.setMcc(null);
                    entity.setMerchantId(merchantId);
                    entity.setStatus("A");
                    entity.setCreatedAt(now);
                    entity.setUpdatedAt(now);
                    entity.setUpdatedBy(updatedBy);
                    CommercePlanItemEntity saved = repository.save(entity);
                    return PlanItemJpaMapper.toDomain(saved);
                }))
        )).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<PlanItem> findByValue(Long planId, String value) {
        return Mono.defer(() -> Mono.fromCallable(() -> {
                    List<CommercePlanItemEntity> results = repository.findByPlanIdAndValue(planId, value,
                            PageRequest.of(0, 1, Sort.by(Sort.Order.desc("planItemId"))));
                    return results.stream().findFirst()
                            .map(PlanItemJpaMapper::toDomain)
                            .orElseThrow(() -> new NoSuchElementException(
                                    "PLAN_ITEM not found: plan=" + planId + " value=" + value));
                }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<String> findExistingValues(Long planId, List<String> values) {
        if (values == null || values.isEmpty()) {
            return Flux.empty();
        }
        return Flux.defer(() -> Flux.fromIterable(repository.findExistingValues(planId, values)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Integer> insertMccBulk(Long planId, List<String> mccs, String by) {
        if (mccs == null || mccs.isEmpty()) {
            return Mono.just(0);
        }
        return Mono.defer(() -> Mono.fromCallable(() ->
                Objects.requireNonNull(txTemplate.execute(status -> {
                    Set<String> existing = new HashSet<>(repository.findExistingValues(planId, mccs));
                    int inserted = 0;
                    for (String mcc : mccs) {
                        if (existing.add(mcc)) {
                            OffsetDateTime now = OffsetDateTime.now();
                            CommercePlanItemEntity entity = new CommercePlanItemEntity();
                            entity.setPlanId(planId);
                            entity.setMcc(mcc);
                            entity.setMerchantId(null);
                            entity.setStatus("A");
                            entity.setCreatedAt(now);
                            entity.setUpdatedAt(now);
                            entity.setUpdatedBy(by);
                            repository.save(entity);
                            inserted++;
                        }
                    }
                    return inserted;
                }))
        )).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Integer> insertMerchantBulk(Long planId, List<String> mids, String by) {
        if (mids == null || mids.isEmpty()) {
            return Mono.just(0);
        }
        return Mono.defer(() -> Mono.fromCallable(() ->
                Objects.requireNonNull(txTemplate.execute(status -> {
                    Set<String> existing = new HashSet<>(repository.findExistingValues(planId, mids));
                    int inserted = 0;
                    for (String mid : mids) {
                        if (existing.add(mid)) {
                            OffsetDateTime now = OffsetDateTime.now();
                            CommercePlanItemEntity entity = new CommercePlanItemEntity();
                            entity.setPlanId(planId);
                            entity.setMcc(null);
                            entity.setMerchantId(mid);
                            entity.setStatus("A");
                            entity.setCreatedAt(now);
                            entity.setUpdatedAt(now);
                            entity.setUpdatedBy(by);
                            repository.save(entity);
                            inserted++;
                        }
                    }
                    return inserted;
                }))
        )).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> existsActiveByPlanId(Long planId) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.existsByPlanIdAndStatus(planId, "A")))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Specification<CommercePlanItemEntity> buildSpecification(Long planId, String status) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("planId"), planId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private CommercePlanItemEntity findLatest(Long planId, String value) {
        List<CommercePlanItemEntity> results = repository.findByPlanIdAndValue(planId, value,
                PageRequest.of(0, 1, Sort.by(Sort.Order.desc("planItemId"))));
        return results.stream().findFirst().orElse(null);
    }
}

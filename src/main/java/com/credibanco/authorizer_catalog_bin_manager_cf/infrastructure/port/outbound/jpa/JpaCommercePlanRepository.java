package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.CommercePlanEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.CommercePlanJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.CommercePlanJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Repository
@Slf4j
public class JpaCommercePlanRepository implements CommercePlanRepository {

    private final CommercePlanJpaRepository repository;
    private final TransactionTemplate txTemplate;

    public JpaCommercePlanRepository(CommercePlanJpaRepository repository,
                                     PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Mono<Boolean> existsByCode(String planCode) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.existsByPlanCode(planCode)))
                .onErrorResume(ex -> handlePlanExistsError(planCode, ex))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<CommercePlan> findByCode(String planCode) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.findByPlanCode(planCode)))
                .onErrorResume(ex -> handlePlanLookupError(planCode, ex))
                .flatMap(optional -> optional
                        .map(entity -> Mono.just(CommercePlanJpaMapper.toDomain(entity)))
                        .orElseGet(Mono::empty))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Optional<CommercePlanEntity>> handlePlanLookupError(String planCode, Throwable error) {
        if (error instanceof DataAccessException && isPlanCodeLengthError(error)) {
            log.warn("find plan by code - ignoring invalid planCode='{}' due to length error: {}",
                    planCode, formatLengthErrorMessage(error));
            return Mono.just(Optional.empty());
        }
        return Mono.error(error);
    }

    private boolean isPlanCodeLengthError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("ORA-00910")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Mono<Boolean> handlePlanExistsError(String planCode, Throwable error) {
        if (error instanceof DataAccessException && isPlanCodeLengthError(error)) {
            log.warn("exists plan by code - ignoring invalid planCode='{}' due to length error: {}",
                    planCode, formatLengthErrorMessage(error));
            return Mono.just(Boolean.FALSE);
        }
        return Mono.error(error);
    }

    private String formatLengthErrorMessage(Throwable error) {
        Throwable current = error;
        Throwable last = error;
        while (current != null) {
            last = current;
            current = current.getCause();
        }
        StringBuilder builder = new StringBuilder(last.getClass().getSimpleName());
        String message = last.getMessage();
        if (message != null && !message.isBlank()) {
            builder.append(": ").append(message.replaceAll("\n", " ").trim());
        }
        return builder.toString();
    }

    @Override
    public Flux<CommercePlan> findAll(String status, String q, int page, int size) {
        return Flux.defer(() -> {
                    Specification<CommercePlanEntity> spec = buildSpecification(status, q);
                    int p = Math.max(0, page);
                    int s = Math.max(1, size);
                    List<CommercePlanEntity> content = repository.findAll(spec,
                                    PageRequest.of(p, s, Sort.by(Sort.Order.asc("planCode"))))
                            .getContent();
                    return Flux.fromIterable(content).map(CommercePlanJpaMapper::toDomain);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<CommercePlan> save(CommercePlan plan) {
        return Mono.defer(() -> Mono.fromCallable(() ->
                Objects.requireNonNull(txTemplate.execute(status -> {
                    CommercePlanEntity entity = CommercePlanJpaMapper.toEntity(plan);
                    if (entity.getPlanId() == null) {
                        repository.findByPlanCode(plan.code()).ifPresent(existing -> {
                            entity.setPlanId(existing.getPlanId());
                            if (entity.getCreatedAt() == null) {
                                entity.setCreatedAt(existing.getCreatedAt());
                            }
                        });
                    }
                    if (entity.getCreatedAt() == null) {
                        entity.setCreatedAt(OffsetDateTime.now());
                    }
                    if (entity.getUpdatedAt() == null) {
                        entity.setUpdatedAt(OffsetDateTime.now());
                    }
                    CommercePlanEntity saved = repository.save(entity);
                    return CommercePlanJpaMapper.toDomain(saved);
                }))
        )).subscribeOn(Schedulers.boundedElastic());
    }

    private Specification<CommercePlanEntity> buildSpecification(String status, String search) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toUpperCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.upper(root.get("planCode")), pattern),
                        cb.like(cb.upper(root.get("planName")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}

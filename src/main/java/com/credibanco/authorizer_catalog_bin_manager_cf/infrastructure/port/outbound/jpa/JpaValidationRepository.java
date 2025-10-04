package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.Validation;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.ValidationEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.ValidationJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.ValidationJpaRepository;
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
import java.util.NoSuchElementException;
import java.util.Objects;

@Repository
public class JpaValidationRepository implements ValidationRepository {

    private final ValidationJpaRepository repository;
    private final TransactionTemplate txTemplate;

    public JpaValidationRepository(ValidationJpaRepository repository,
                                   PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Mono<Boolean> existsByCode(String code) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.existsByCode(code)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Validation> save(Validation v) {
        return Mono.defer(() -> Mono.fromCallable(() ->
                Objects.requireNonNull(txTemplate.execute(status -> {
                    ValidationEntity entity = ValidationJpaMapper.toEntity(v);
                    repository.findByCode(v.code()).ifPresent(existing -> {
                        entity.setValidationId(existing.getValidationId());
                        if (entity.getCreatedAt() == null) {
                            entity.setCreatedAt(existing.getCreatedAt());
                        }
                        if (entity.getValidFrom() == null) {
                            entity.setValidFrom(existing.getValidFrom());
                        }
                    });
                    if (entity.getCreatedAt() == null) {
                        entity.setCreatedAt(OffsetDateTime.now());
                    }
                    if (entity.getUpdatedAt() == null) {
                        entity.setUpdatedAt(OffsetDateTime.now());
                    }
                    ValidationEntity saved = repository.save(entity);
                    return ValidationJpaMapper.toDomain(saved);
                }))
        )).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Validation> findByCode(String code) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.findByCode(code)
                        .map(ValidationJpaMapper::toDomain)
                        .orElseThrow(() -> new NoSuchElementException("VALIDATION not found: " + code))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Validation> findById(Long id) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.findById(id)
                        .map(ValidationJpaMapper::toDomain)
                        .orElseThrow(() -> new NoSuchElementException("VALIDATION not found: " + id))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Validation> findAll(String status, String search, int page, int size) {
        return Flux.defer(() -> {
                    Specification<ValidationEntity> spec = buildSpecification(status, search);
                    int p = Math.max(0, page);
                    int s = Math.max(1, size);
                    List<ValidationEntity> content = repository.findAll(spec,
                                    PageRequest.of(p, s, Sort.by(Sort.Order.asc("code"))))
                            .getContent();
                    return Flux.fromIterable(content).map(ValidationJpaMapper::toDomain);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Specification<ValidationEntity> buildSpecification(String status, String search) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toUpperCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.upper(root.get("code")), pattern),
                        cb.like(cb.upper(root.get("description")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}

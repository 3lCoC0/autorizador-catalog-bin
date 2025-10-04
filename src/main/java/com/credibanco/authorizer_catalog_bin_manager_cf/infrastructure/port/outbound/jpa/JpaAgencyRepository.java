package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.agency.port.outbound.AgencyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.AgencyReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.agency.Agency;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.AgencyEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.AgencyEntityId;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.AgencyJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.AgencyJpaRepository;
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
public class JpaAgencyRepository implements AgencyRepository, AgencyReadOnlyRepository {

    private final AgencyJpaRepository repository;
    private final TransactionTemplate txTemplate;

    public JpaAgencyRepository(AgencyJpaRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Mono<Boolean> existsByPk(String subtypeCode, String agencyCode) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.existsById(new AgencyEntityId(subtypeCode, agencyCode))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Agency> save(Agency aggregate) {
        return Mono.defer(() -> Mono.fromCallable(() ->
                Objects.requireNonNull(txTemplate.execute(status -> {
                    AgencyEntity entity = AgencyJpaMapper.toEntity(aggregate);
                    if (entity.getCreatedAt() == null) {
                        OffsetDateTime now = OffsetDateTime.now();
                        entity.setCreatedAt(now);
                        entity.setUpdatedAt(now);
                    }
                    AgencyEntity saved = repository.save(entity);
                    return AgencyJpaMapper.toDomain(saved);
                }))
        )).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Agency> findByPk(String subtypeCode, String agencyCode) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.findById(new AgencyEntityId(subtypeCode, agencyCode))
                        .map(AgencyJpaMapper::toDomain)
                        .orElseThrow(() -> new NoSuchElementException(
                                "AGENCY not found: subtype=" + subtypeCode + " agency=" + agencyCode)))
        ).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Agency> findAll(String subtypeCode, String status, String search, int page, int size) {
        return Flux.defer(() -> {
                    Specification<AgencyEntity> spec = buildSpecification(subtypeCode, status, search);
                    int p = Math.max(0, page);
                    int s = Math.max(1, size);
                    List<AgencyEntity> content = repository.findAll(spec,
                                    PageRequest.of(p, s, Sort.by(Sort.Order.asc("id.subtypeCode"), Sort.Order.asc("id.agencyCode"))))
                            .getContent();
                    return Flux.fromIterable(content).map(AgencyJpaMapper::toDomain);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> existsAnotherActive(String subtypeCode, String excludeAgencyCode) {
        return Mono.defer(() -> Mono.fromCallable(() ->
                        repository.existsByIdSubtypeCodeAndStatusAndIdAgencyCodeNot(subtypeCode, "A", excludeAgencyCode)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Long> countActiveBySubtypeCode(String subtypeCode) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.countByIdSubtypeCodeAndStatus(subtypeCode, "A")))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Specification<AgencyEntity> buildSpecification(String subtypeCode, String status, String search) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (subtypeCode != null) {
                predicates.add(cb.equal(root.get("id").get("subtypeCode"), subtypeCode));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toUpperCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.upper(root.get("name")), pattern),
                        cb.like(cb.upper(root.get("id").get("agencyCode")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.SubtypeRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.subtype.Subtype;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeEntityId;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.SubtypeJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.SubtypeJpaRepository;
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
import java.util.NoSuchElementException;
import java.util.Objects;

@Repository
public class JpaSubtypeRepository implements SubtypeRepository {

    private final SubtypeJpaRepository repository;
    private final TransactionTemplate txTemplate;

    public JpaSubtypeRepository(SubtypeJpaRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Mono<Boolean> existsByPk(String bin, String subtypeCode) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.existsById(new SubtypeEntityId(subtypeCode, bin))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> existsByBinAndExt(String bin, String binExt) {
        if (binExt == null) {
            return Mono.just(false);
        }
        return Mono.defer(() -> Mono.fromCallable(() -> repository.existsByIdBinAndBinExt(bin, binExt)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Subtype> save(Subtype entity) {
        return Mono.defer(() -> Mono.fromCallable(() ->
                Objects.requireNonNull(txTemplate.execute(status -> {
                    SubtypeEntity toPersist = SubtypeJpaMapper.toEntity(entity);
                    if (toPersist.getCreatedAt() == null) {
                        OffsetDateTime now = OffsetDateTime.now();
                        toPersist.setCreatedAt(now);
                        toPersist.setUpdatedAt(now);
                    }
                    SubtypeEntity saved = repository.save(toPersist);
                    return SubtypeJpaMapper.toDomain(saved);
                }))
        )).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Subtype> findByPk(String bin, String subtypeCode) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.findById(new SubtypeEntityId(subtypeCode, bin))
                        .map(SubtypeJpaMapper::toDomain)
                        .orElseThrow(() -> new NoSuchElementException(
                                "SUBTYPE not found: bin=" + bin + " code=" + subtypeCode)))
        ).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Subtype> findAll(String bin, String code, String status, int page, int size) {
        return Flux.defer(() -> {
                    Specification<SubtypeEntity> spec = buildSpecification(bin, code, status);
                    int p = Math.max(0, page);
                    int s = Math.max(1, size);
                    List<SubtypeEntity> content = repository.findAll(spec,
                                    PageRequest.of(p, s, Sort.by(Sort.Order.asc("id.bin"), Sort.Order.asc("id.subtypeCode"))))
                            .getContent();
                    return Flux.fromIterable(content).map(SubtypeJpaMapper::toDomain);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Specification<SubtypeEntity> buildSpecification(String bin, String code, String status) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (bin != null) {
                predicates.add(cb.equal(root.get("id").get("bin"), bin));
            }
            if (code != null) {
                predicates.add(cb.equal(root.get("id").get("subtypeCode"), code));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}

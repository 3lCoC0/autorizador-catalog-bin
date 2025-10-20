package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.rule.port.outbound.ValidationMapRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.rule.ValidationMap;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.SubtypeEntityId;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.ValidationMapEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.ValidationMapJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.SubtypeJpaRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.ValidationJpaRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.ValidationMapJpaRepository;
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
public class JpaValidationMapRepository implements ValidationMapRepository {

    private final ValidationMapJpaRepository repository;
    private final SubtypeJpaRepository subtypeRepository;
    private final ValidationJpaRepository validationRepository;
    private final TransactionTemplate txTemplate;

    public JpaValidationMapRepository(ValidationMapJpaRepository repository,
                                      SubtypeJpaRepository subtypeRepository,
                                      ValidationJpaRepository validationRepository,
                                      PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.subtypeRepository = subtypeRepository;
        this.validationRepository = validationRepository;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Mono<Boolean> existsActive(String subtypeCode, String bin, Long validationId) {
        return Mono.defer(() -> Mono.fromCallable(() ->
                        repository.existsBySubtypeCodeAndBinAndValidationIdAndStatus(subtypeCode, bin, validationId, "A")))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ValidationMap> save(ValidationMap map) {
        return Mono.defer(() -> Mono.fromCallable(() ->
                Objects.requireNonNull(txTemplate.execute(status -> {
                    var id = new SubtypeEntityId(map.subtypeCode(), map.bin());
                    if (!subtypeRepository.existsById(id)) {
                        throw new NoSuchElementException("No existe SUBTYPE(subtypeCode,bin) o la VALIDATION no está activa/vigente");
                    }
                    boolean validationActive = validationRepository
                            .findActiveById(map.validationId(), OffsetDateTime.now())
                            .isPresent();
                    if (!validationActive) {
                        throw new NoSuchElementException("No existe SUBTYPE(subtypeCode,bin) o la VALIDATION no está activa/vigente");
                    }
                    ValidationMapEntity entity = repository
                            .findBySubtypeCodeAndBinAndValidationId(map.subtypeCode(), map.bin(), map.validationId())
                            .orElseGet(ValidationMapEntity::new);
                    boolean isNew = entity.getMapId() == null;
                    entity.setSubtypeCode(map.subtypeCode());
                    entity.setBin(map.bin());
                    entity.setValidationId(map.validationId());
                    entity.setStatus(map.status());
                    entity.setValueFlag(map.valueFlag());
                    entity.setValueNum(map.valueNum());
                    entity.setValueText(map.valueText());
                    OffsetDateTime now = OffsetDateTime.now();
                    if (isNew) {
                        entity.setCreatedAt(map.createdAt() != null ? map.createdAt() : now);
                    }
                    entity.setUpdatedAt(map.updatedAt() != null ? map.updatedAt() : now);
                    entity.setUpdatedBy(map.updatedBy());
                    ValidationMapEntity saved = repository.save(entity);
                    return ValidationMapJpaMapper.toDomain(saved);
                }))
        )).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ValidationMap> findByNaturalKey(String subtypeCode, String bin, Long validationId) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository
                                .findBySubtypeCodeAndBinAndValidationId(subtypeCode, bin, validationId))
                        .flatMap(optional -> optional
                                .map(ValidationMapJpaMapper::toDomain)
                                .map(Mono::just)
                                .orElseGet(Mono::empty)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<ValidationMap> findAll(String subtypeCode, String bin, String status, int page, int size) {
        return Flux.defer(() -> {
                    Specification<ValidationMapEntity> spec = buildSpecification(subtypeCode, bin, status);
                    int p = Math.max(0, page);
                    int s = Math.max(1, size);
                    List<ValidationMapEntity> content = repository.findAll(spec,
                                    PageRequest.of(p, s, Sort.by(Sort.Order.asc("subtypeCode"),
                                            Sort.Order.asc("bin"), Sort.Order.asc("validationId"))))
                            .getContent();
                    return Flux.fromIterable(content).map(ValidationMapJpaMapper::toDomain);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<ValidationMap> findResolved(String subtypeCode, String bin, String status, int page, int size) {
        return Flux.defer(() -> {
                    int p = Math.max(0, page);
                    int s = Math.max(1, size);
                    List<ValidationMapEntity> content = repository.findResolved(subtypeCode, bin, status,
                            PageRequest.of(p, s, Sort.by(Sort.Order.asc("subtypeCode"),
                                    Sort.Order.asc("bin"), Sort.Order.asc("validationId"))));
                    return Flux.fromIterable(content).map(ValidationMapJpaMapper::toDomain);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Specification<ValidationMapEntity> buildSpecification(String subtypeCode, String bin, String status) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (subtypeCode != null) {
                predicates.add(cb.equal(root.get("subtypeCode"), subtypeCode));
            }
            if (bin != null) {
                predicates.add(cb.equal(root.get("bin"), bin));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}

package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.subtype.port.outbound.BinReadOnlyRepository.BinExtConfig;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.entity.BinEntity;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.mapper.BinJpaMapper;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.jpa.repository.BinJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Repository
public class JpaBinRepository implements BinRepository, BinReadOnlyRepository {

    private final BinJpaRepository repository;
    private final TransactionTemplate txTemplate;

    public JpaBinRepository(BinJpaRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Mono<Boolean> existsById(String bin) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.existsById(bin)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Bin> save(Bin bin) {
        return Mono.defer(() -> Mono.fromCallable(() ->
                Objects.requireNonNull(txTemplate.execute(status -> {
                    BinEntity entity = BinJpaMapper.toEntity(bin);
                    BinEntity saved = repository.save(entity);
                    return BinJpaMapper.toDomain(saved);
                }))
        )).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Bin> findById(String bin) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.findById(bin)
                        .map(BinJpaMapper::toDomain)
                        .orElseThrow(() -> new NoSuchElementException("BIN not found: " + bin))))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<Bin> findAll(int page, int size) {
        return Flux.defer(() -> {
                    int p = Math.max(0, page);
                    int s = Math.max(1, size);
                    List<BinEntity> content = repository
                            .findAll(PageRequest.of(p, s, Sort.by("bin").ascending()))
                            .getContent();
                    return Flux.fromIterable(content)
                            .map(BinJpaMapper::toDomain);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<BinExtConfig> getExtConfig(String bin) {
        return Mono.defer(() -> Mono.fromCallable(() -> repository.findById(bin)
                        .map(e -> new BinExtConfig(e.getUsesBinExt(), e.getBinExtDigits()))
                        .orElseThrow(() -> new NoSuchElementException("BIN not found: " + bin))))
                .subscribeOn(Schedulers.boundedElastic());
    }
}

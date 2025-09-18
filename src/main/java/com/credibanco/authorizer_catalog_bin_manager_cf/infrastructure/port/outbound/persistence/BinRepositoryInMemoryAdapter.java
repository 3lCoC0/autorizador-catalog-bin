package com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.port.outbound.persistence;

import com.credibanco.authorizer_catalog_bin_manager_cf.domain.bin.Bin;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.bin.port.outbound.BinRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public final class BinRepositoryInMemoryAdapter implements BinRepository {

    private final Map<String, Bin> store = new ConcurrentHashMap<>();

    @Override
    public Mono<Boolean> existsById(String bin) {
        return Mono.fromSupplier(() -> store.containsKey(bin));
    }

    @Override
    public Mono<Bin> save(Bin bin) {
        return Mono.fromSupplier(() -> {
            store.put(bin.bin(), bin);
            return bin;
        });
    }

    @Override
    public Mono<Bin> findById(String bin) {
        return Mono.defer(() -> {
            Bin b = store.get(bin);
            return b == null ? Mono.empty() : Mono.just(b);
        });
    }

    @Override
    public Flux<Bin> findAll(int page, int size) {
        return Flux.defer(() -> {
            int p = Math.max(0, page);
            int s = Math.max(1, size);
            int from = p * s;
            var list = new ArrayList<>(store.values());
            list.sort(Comparator.comparing(Bin::bin));
            int to = Math.min(list.size(), from + s);
            if (from >= to) return Flux.empty();
            return Flux.fromIterable(list.subList(from, to));
        });
    }

    public void clear() { store.clear(); }
}

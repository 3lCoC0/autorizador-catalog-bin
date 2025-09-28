package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.model.PlanItemsBulkResult;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.AddPlanItemUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.*;


import java.util.NoSuchElementException;

@Slf4j
public record AddPlanItemService(CommercePlanRepository planRepo,
                                 CommercePlanItemRepository itemRepo,
                                 TransactionalOperator tx) implements AddPlanItemUseCase {

    private static final int BATCH_SIZE = 500;

    @Override
    public Mono<PlanItem> addValue(String planCode, String value, String by) {
        log.info("AddPlanItemService IN planCode={} value={} by={}", planCode, value, by);
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Plan no existe")))
                .flatMap(p -> {
                    if (p.validationMode() == CommerceValidationMode.MCC) {
                        if (!value.matches("^\\d{4}$")) {
                            return Mono.error(new IllegalArgumentException("Para modo MCC, 'value' debe ser 4 dígitos"));
                        }
                    } else if (p.validationMode() == CommerceValidationMode.MERCHANT_ID) {
                        if (!value.matches("^\\d{9}$")) {
                            return Mono.error(new IllegalArgumentException("Para modo MERCHANT_ID, 'value' debe ser 9 dígitos"));
                        }
                    }
                    return itemRepo.findByValue(p.planId(), value)
                            .flatMap(existing -> Mono.<PlanItem>error(new IllegalStateException("Ítem ya existe")))
                            .switchIfEmpty(
                                    (p.validationMode() == CommerceValidationMode.MCC)
                                            ? itemRepo.insertMcc(p.planId(), value, by)
                                            : itemRepo.insertMerchant(p.planId(), value, by)
                            );
                })
                .doOnSuccess(pi -> log.info("AddPlanItemService OK planId={} itemId={}", pi.planId(), pi.planItemId()))
                .as(tx::transactional)
                .onErrorMap(org.springframework.dao.DuplicateKeyException.class,
                        e -> new IllegalStateException("Ítem ya existe"));
    }

    @Override
    public Mono<PlanItemsBulkResult> addMany(String planCode, List<String> rawValues, String by) {
        log.info("AddPlanItemService IN (bulk) planCode={} count={} by={}", planCode,
                rawValues == null ? 0 : rawValues.size(), by);

        List<String> cleaned = (rawValues == null ? List.<String>of() : rawValues).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Plan no existe")))
                .flatMap(plan -> {
                    var mode = plan.validationMode();


                    List<String> invalid = cleaned.stream()
                            .filter(v -> !isValid(mode, v))
                            .toList();
                    Set<String> invalidSet = new HashSet<>(invalid);


                    List<String> candidates = cleaned.stream()
                            .filter(v -> !invalidSet.contains(v))
                            .toList();

                    return existingValues(plan.planId(), candidates)
                            .collectList()
                            .flatMap(existing -> {
                                Set<String> existingSet = new HashSet<>(existing);
                                List<String> duplicates = candidates.stream()
                                        .filter(existingSet::contains)
                                        .toList();
                                List<String> toInsert = candidates.stream()
                                        .filter(v -> !existingSet.contains(v))
                                        .toList();

                                if (toInsert.isEmpty()) {
                                    return Mono.just(new PlanItemsBulkResult(
                                            plan.code(),
                                            rawValues == null ? 0 : rawValues.size(),
                                            0,
                                            duplicates.size(),
                                            invalid.size(),
                                            invalid,
                                            duplicates
                                    ));
                                }

                                return insertBatches(plan.planId(), mode, toInsert, by)
                                        .map(inserted -> new PlanItemsBulkResult(
                                                plan.code(),
                                                rawValues == null ? 0 : rawValues.size(),
                                                inserted,
                                                duplicates.size(),
                                                invalid.size(),
                                                invalid,
                                                duplicates
                                        ));
                            });
                })
                .doOnSuccess(r -> log.info("AddPlanItemService OK (bulk) planCode={} inserted={} dup={} invalid={}",
                        r.planCode(), r.inserted(), r.duplicates(), r.invalid()))
                .as(tx::transactional);
    }

    private boolean isValid(CommerceValidationMode mode, String v) {
        return switch (mode) {
            case MCC -> v.matches("^\\d{4}$");
            case MERCHANT_ID -> v.matches("^\\d{9}$");
        };
    }

    private Flux<String> existingValues(Long planId, List<String> values) {
        if (values.isEmpty()) return Flux.empty();
        List<List<String>> chunks = chunk(values, BATCH_SIZE);
        return Flux.fromIterable(chunks)
                .concatMap(chunk -> itemRepo.findExistingValues(planId, chunk));
    }

    private Mono<Integer> insertBatches(Long planId, CommerceValidationMode mode, List<String> values, String by) {
        List<List<String>> chunks = chunk(values, BATCH_SIZE);
        return Flux.fromIterable(chunks)
                .concatMap(chunk -> (mode == CommerceValidationMode.MCC)
                        ? itemRepo.insertMccBulk(planId, chunk, by)
                        : itemRepo.insertMerchantBulk(planId, chunk, by))
                .reduce(0, Integer::sum);
    }

    private static <T> List<List<T>> chunk(List<T> list, int size) {
        if (list.isEmpty()) return List.of();
        int n = (list.size() + size - 1) / size;
        List<List<T>> parts = new ArrayList<>(n);
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }
}
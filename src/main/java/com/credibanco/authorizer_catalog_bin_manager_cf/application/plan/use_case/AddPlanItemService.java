package com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.use_case;

import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.model.PlanItemsBulkResult;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.inbound.AddPlanItemUseCase;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanItemRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.application.plan.port.outbound.CommercePlanRepository;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommerceValidationMode;
import com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.PlanItem;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppError;
import com.credibanco.authorizer_catalog_bin_manager_cf.infrastructure.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.stream.Stream;

import java.util.*;

@Slf4j
public record AddPlanItemService(CommercePlanRepository planRepo,
                                 CommercePlanItemRepository itemRepo,
                                 TransactionalOperator tx) implements AddPlanItemUseCase {

    private static final int BATCH_SIZE = 500;

    @Override
    public Mono<PlanItem> addValue(String planCode, String value, String by) {
        log.info("AddPlanItemService IN planCode={} value={} by={}", planCode, value, by);
        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.<com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan>error(
                        new AppException(AppError.PLAN_NOT_FOUND)))
                .flatMap(p -> {

                    if (p.validationMode() == CommerceValidationMode.MCC) {
                        if (value == null || !value.matches("^\\d{4}$")) {
                            return Mono.<PlanItem>error(new AppException(AppError.PLAN_ITEM_INVALID_DATA,
                                    "Para modo MCC, 'value' debe ser 4 dígitos"));
                        }
                    } else if (p.validationMode() == CommerceValidationMode.MERCHANT_ID) {
                        if (value == null || !value.matches("^\\d{9}$")) {
                            return Mono.<PlanItem>error(new AppException(AppError.PLAN_ITEM_INVALID_DATA,
                                    "Para modo MERCHANT_ID, 'value' debe ser 9 dígitos"));
                        }
                    }
                    return itemRepo.findByValue(p.planId(), value)
                            .flatMap(existing -> Mono.<PlanItem>error(
                                    new AppException(AppError.PLAN_ITEM_INVALID_DATA, "Ítem ya existe")))
                            .switchIfEmpty(
                                    (p.validationMode() == CommerceValidationMode.MCC)
                                            ? itemRepo.insertMcc(p.planId(), value, by)
                                            : itemRepo.insertMerchant(p.planId(), value, by)
                            );
                })
                .doOnSuccess(pi -> log.info("AddPlanItemService OK planId={} itemId={}", pi.planId(), pi.planItemId()))
                .as(tx::transactional)
                .onErrorMap(org.springframework.dao.DuplicateKeyException.class,
                        e -> new AppException(AppError.PLAN_ITEM_INVALID_DATA, "Ítem ya existe"));
    }

    @Override
    public Mono<PlanItemsBulkResult> addMany(String planCode, List<String> rawValues, String by) {
        List<String> original = rawValues == null ? List.of() : rawValues;
        log.info("AddPlanItemService IN (bulk) planCode={} count={} by={}", planCode,
                original.size(), by);

        List<String> normalized = new ArrayList<>();
        List<String> invalidNullOrBlank = new ArrayList<>();

        for (String value : original) {
            if (value == null) {
                invalidNullOrBlank.add("null");
                continue;
            }

            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                invalidNullOrBlank.add(value);
                continue;
            }

            normalized.add(trimmed);
        }

        List<String> cleaned = normalized.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        return planRepo.findByCode(planCode)
                .switchIfEmpty(Mono.<com.credibanco.authorizer_catalog_bin_manager_cf.domain.plan.CommercePlan>error(
                        new AppException(AppError.PLAN_NOT_FOUND)))
                .flatMap(plan -> {
                    var mode = plan.validationMode();
                    List<String> invalidByMode = cleaned.stream()
                            .filter(v -> !isValid(mode, v))
                            .toList();
                    List<String> invalid = Stream.concat(invalidNullOrBlank.stream(), invalidByMode.stream())
                            .toList();
                    Set<String> invalidSet = new HashSet<>(invalidByMode);

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
                                            original.size(),
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
                                                original.size(),
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
            case MCC -> v != null && v.matches("^\\d{4}$");
            case MERCHANT_ID -> v != null && v.matches("^\\d{9}$");
        };
    }

    private Flux<String> existingValues(Long planId, List<String> values) {
        if (values.isEmpty()) return Flux.empty();
        List<List<String>> chunks = chunk(values);
        return Flux.fromIterable(chunks)
                .concatMap(chunk -> itemRepo.findExistingValues(planId, chunk));
    }

    private Mono<Integer> insertBatches(Long planId, CommerceValidationMode mode, List<String> values, String by) {
        List<List<String>> chunks = chunk(values);
        return Flux.fromIterable(chunks)
                .concatMap(chunk -> (mode == CommerceValidationMode.MCC)
                        ? itemRepo.insertMccBulk(planId, chunk, by)
                        : itemRepo.insertMerchantBulk(planId, chunk, by))
                .reduce(0, Integer::sum);
    }

    private static <T> List<List<T>> chunk(List<T> list) {
        if (list.isEmpty()) return List.of();
        int n = (list.size() + AddPlanItemService.BATCH_SIZE - 1) / AddPlanItemService.BATCH_SIZE;
        List<List<T>> parts = new ArrayList<>(n);
        for (int i = 0; i < list.size(); i += AddPlanItemService.BATCH_SIZE) {
            parts.add(list.subList(i, Math.min(i + AddPlanItemService.BATCH_SIZE, list.size())));
        }
        return parts;
    }
}

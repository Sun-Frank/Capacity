package com.capics.service;

import com.capics.entity.CtLineData;
import com.capics.entity.LineConfig;
import com.capics.entity.LineProfile;
import com.capics.entity.Product;
import com.capics.entity.ProductFamily;
import com.capics.entity.ProductId;
import com.capics.entity.Routing;
import com.capics.entity.RoutingItem;
import com.capics.repository.CtLineDataRepository;
import com.capics.repository.LineConfigRepository;
import com.capics.repository.LineProfileRepository;
import com.capics.repository.ProductFamilyRepository;
import com.capics.repository.ProductRepository;
import com.capics.repository.RoutingItemRepository;
import com.capics.repository.RoutingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CapacityAssessmentService {

    private static final BigDecimal SEC_PER_HOUR = BigDecimal.valueOf(3600);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal SEVEN = BigDecimal.valueOf(7);
    private static final long RESULT_CACHE_TTL_MS = 3 * 60 * 1000;
    private static final int RESULT_CACHE_MAX_SIZE = 64;

    private final MrpPlanService mrpPlanService;
    private final RoutingRepository routingRepository;
    private final RoutingItemRepository routingItemRepository;
    private final ProductRepository productRepository;
    private final ProductFamilyRepository productFamilyRepository;
    private final CtLineDataRepository ctLineDataRepository;
    private final LineConfigRepository lineConfigRepository;
    private final LineProfileRepository lineProfileRepository;
    private final ManpowerPlanService manpowerPlanService;
    private final ConcurrentHashMap<String, CacheEntry> weeklyResultCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry> monthlyResultCache = new ConcurrentHashMap<>();

    public CapacityAssessmentService(
            MrpPlanService mrpPlanService,
            RoutingRepository routingRepository,
            RoutingItemRepository routingItemRepository,
            ProductRepository productRepository,
            ProductFamilyRepository productFamilyRepository,
            CtLineDataRepository ctLineDataRepository,
            LineConfigRepository lineConfigRepository,
            LineProfileRepository lineProfileRepository,
            ManpowerPlanService manpowerPlanService) {
        this.mrpPlanService = mrpPlanService;
        this.routingRepository = routingRepository;
        this.routingItemRepository = routingItemRepository;
        this.productRepository = productRepository;
        this.productFamilyRepository = productFamilyRepository;
        this.ctLineDataRepository = ctLineDataRepository;
        this.lineConfigRepository = lineConfigRepository;
        this.lineProfileRepository = lineProfileRepository;
        this.manpowerPlanService = manpowerPlanService;
    }

    public Map<String, Object> getCapacityAssessment(String createdBy, String fileName, String version) {
        String cacheKey = buildResultCacheKey(createdBy, fileName, version);
        Map<String, Object> cached = getCachedResult(weeklyResultCache, cacheKey);
        if (cached != null) {
            return cached;
        }

        LinkedHashSet<String> warningSet = new LinkedHashSet<>();
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> mrpData = mrpPlanService.getWeeklyDemandByVersion(createdBy, fileName, version);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mrpItems = (List<Map<String, Object>>) mrpData.get("data");
        @SuppressWarnings("unchecked")
        Map<String, LocalDate[]> weekDateRanges = (Map<String, LocalDate[]>) mrpData.get("weekDateRanges");

        if (mrpItems == null || mrpItems.isEmpty()) {
            result.put("lines", new LinkedHashMap<>());
            result.put("weeks", new ArrayList<>());
            result.put("weekDates", new LinkedHashMap<>());
            result.put("warnings", new ArrayList<>(warningSet));
            return result;
        }

        List<String> weeks = new ArrayList<>(weekDateRanges.keySet());
        Map<String, String> weekDates = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
        for (Map.Entry<String, LocalDate[]> entry : weekDateRanges.entrySet()) {
            weekDates.put(entry.getKey(), entry.getValue()[0].format(formatter));
        }

        RequestCache cache = buildRequestCache(mrpItems, warningSet);

        Map<String, List<Map<String, Object>>> linesData = new LinkedHashMap<>();
        for (Map<String, Object> mrpItem : mrpItems) {
            String itemNumber = (String) mrpItem.get("itemNumber");
            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> weeklyDemand = (Map<String, BigDecimal>) mrpItem.get("weeks");
            processOneFinishedProductWeekly(itemNumber, weeklyDemand, weeks, weekDateRanges, linesData, cache);
        }

        result.put("lines", linesData);
        result.put("weeks", weeks);
        result.put("weekDates", weekDates);
        result.put("warnings", new ArrayList<>(warningSet));
        putCachedResult(weeklyResultCache, cacheKey, result);
        return result;
    }

    public Map<String, Object> getCapacityAssessmentMonthly(String createdBy, String fileName, String version) {
        String cacheKey = buildResultCacheKey(createdBy, fileName, version);
        Map<String, Object> cached = getCachedResult(monthlyResultCache, cacheKey);
        if (cached != null) {
            return cached;
        }

        LinkedHashSet<String> warningSet = new LinkedHashSet<>();
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> mrpData = mrpPlanService.getMonthlyDemandByVersion(createdBy, fileName, version);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mrpItems = (List<Map<String, Object>>) mrpData.get("data");
        @SuppressWarnings("unchecked")
        Map<String, String[]> monthDateRanges = (Map<String, String[]>) mrpData.get("monthDateRanges");

        if (mrpItems == null || mrpItems.isEmpty()) {
            result.put("lines", new LinkedHashMap<>());
            result.put("months", new ArrayList<>());
            result.put("monthDates", new LinkedHashMap<>());
            result.put("warnings", new ArrayList<>(warningSet));
            return result;
        }

        List<String> months = new ArrayList<>(monthDateRanges.keySet());
        Map<String, String> monthDates = new LinkedHashMap<>();
        Map<String, Integer> monthDays = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : monthDateRanges.entrySet()) {
            String monthKey = entry.getKey();
            String[] dateRange = entry.getValue();
            LocalDate lastDay = LocalDate.parse(dateRange[1]);
            monthDays.put(monthKey, lastDay.getDayOfMonth());
            monthDates.put(monthKey, monthKey.replace("-", "/"));
        }

        RequestCache cache = buildRequestCache(mrpItems, warningSet);

        Map<String, List<Map<String, Object>>> linesData = new LinkedHashMap<>();
        for (Map<String, Object> mrpItem : mrpItems) {
            String itemNumber = (String) mrpItem.get("itemNumber");
            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> monthlyDemand = (Map<String, BigDecimal>) mrpItem.get("months");
            processOneFinishedProductMonthly(itemNumber, monthlyDemand, months, monthDays, linesData, cache);
        }

        result.put("lines", linesData);
        result.put("months", months);
        result.put("monthDates", monthDates);
        result.put("monthDays", monthDays);
        result.put("warnings", new ArrayList<>(warningSet));
        putCachedResult(monthlyResultCache, cacheKey, result);
        return result;
    }

    private RequestCache buildRequestCache(
            List<Map<String, Object>> mrpItems,
            LinkedHashSet<String> warningSet) {

        Set<String> finishedItems = mrpItems.stream()
                .map(item -> (String) item.get("itemNumber"))
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, List<String>> componentsByFinishedItem = new HashMap<>();
        if (finishedItems.isEmpty()) {
            return new RequestCache(componentsByFinishedItem, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
        }

        Map<String, String> itemDescriptionByItemNumber = productRepository.findByItemNumberIn(new ArrayList<>(finishedItems)).stream()
                .filter(p -> p.getItemNumber() != null && p.getDescription() != null && !p.getDescription().isBlank())
                .collect(Collectors.toMap(
                        Product::getItemNumber,
                        Product::getDescription,
                        (first, second) -> first,
                        LinkedHashMap::new));

        List<Routing> routings = routingRepository.findByProductNumberIn(new ArrayList<>(finishedItems));
        Map<String, List<Long>> routingIdsByProduct = new HashMap<>();
        Set<Long> routingIds = new LinkedHashSet<>();
        for (Routing routing : routings) {
            routingIdsByProduct.computeIfAbsent(routing.getProductNumber(), k -> new ArrayList<>()).add(routing.getId());
            routingIds.add(routing.getId());
        }

        Map<Long, List<RoutingItem>> routingItemsByRoutingId = new HashMap<>();
        if (!routingIds.isEmpty()) {
            List<RoutingItem> allRoutingItems = routingItemRepository.findByRoutingIdIn(new ArrayList<>(routingIds));
            for (RoutingItem item : allRoutingItems) {
                routingItemsByRoutingId.computeIfAbsent(item.getRoutingId(), k -> new ArrayList<>()).add(item);
            }
        }

        Set<String> componentNumbers = new LinkedHashSet<>();
        for (String finishedItem : finishedItems) {
            List<Long> ids = routingIdsByProduct.getOrDefault(finishedItem, Collections.emptyList());
            if (ids.isEmpty()) {
                warningSet.add("成品 [" + finishedItem + "] 在工艺路线中未找到");
                componentsByFinishedItem.put(finishedItem, Collections.emptyList());
                continue;
            }

            Set<String> dedupComponents = new LinkedHashSet<>();
            for (Long routingId : ids) {
                List<RoutingItem> items = routingItemsByRoutingId.getOrDefault(routingId, Collections.emptyList());
                for (RoutingItem item : items) {
                    if (item.getComponentNumber() != null && !item.getComponentNumber().isBlank()) {
                        dedupComponents.add(item.getComponentNumber());
                    }
                }
            }
            List<String> components = new ArrayList<>(dedupComponents);
            componentsByFinishedItem.put(finishedItem, components);
            componentNumbers.addAll(components);
        }

        Map<String, List<CtLineData>> mappingsByComponent = new HashMap<>();
        if (!componentNumbers.isEmpty()) {
            List<CtLineData> allMappings = ctLineDataRepository.findByColDAndColCInOrderByIdDesc("主", new ArrayList<>(componentNumbers));
            for (CtLineData mapping : allMappings) {
                String component = normalize(mapping.getColC());
                if (component == null) {
                    continue;
                }
                mappingsByComponent.computeIfAbsent(component, k -> new ArrayList<>()).add(mapping);
            }
        }

        Set<String> lineCodes = mappingsByComponent.values().stream()
                .flatMap(List::stream)
                .map(m -> normalize(m.getColB()))
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, LineConfig> lineConfigByCode = lineConfigRepository.findAllById(lineCodes).stream()
                .collect(Collectors.toMap(LineConfig::getLineCode, v -> v, (a, b) -> a));

        Map<String, String> lineClassByCode = lineProfileRepository.findAllById(lineCodes).stream()
                .collect(Collectors.toMap(LineProfile::getLineCode, LineProfile::getLineClass, (a, b) -> a));

        Set<ProductId> productIds = new LinkedHashSet<>();
        for (List<CtLineData> mappings : mappingsByComponent.values()) {
            for (CtLineData mapping : mappings) {
                String lineCode = normalize(mapping.getColB());
                String component = normalize(mapping.getColC());
                if (lineCode != null && component != null) {
                    productIds.add(new ProductId(component, lineCode));
                }
            }
        }
        Map<ProductId, Product> productById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(
                        p -> new ProductId(p.getItemNumber(), p.getLineCode()),
                        v -> v,
                        (a, b) -> a));

        Map<String, Optional<String>> pfCacheByFamilyLine = new HashMap<>();
        Map<String, List<ResolvedContext>> contextsByComponent = new HashMap<>();

        for (String component : componentNumbers) {
            List<CtLineData> mappings = mappingsByComponent.getOrDefault(component, Collections.emptyList());
            if (mappings.isEmpty()) {
                warningSet.add("组件 [" + component + "] 在产线-产品表（主线）未找到对应关系");
                contextsByComponent.put(component, Collections.emptyList());
                continue;
            }

            Set<String> dedupLineCodes = new HashSet<>();
            List<ResolvedContext> contexts = new ArrayList<>();
            for (CtLineData mapping : mappings) {
                String lineCode = normalize(mapping.getColB());
                if (lineCode == null || !dedupLineCodes.add(lineCode)) {
                    continue;
                }

                LineConfig lineConfig = lineConfigByCode.get(lineCode);
                if (lineConfig == null || !Boolean.TRUE.equals(lineConfig.getIsActive())) {
                    warningSet.add("产线 [" + lineCode + "] 未启用，组件 [" + component + "] 已跳过");
                    continue;
                }

                Integer workers = tryParseInteger(mapping.getColP());
                BigDecimal ct = tryParseDecimal(mapping.getColF());
                BigDecimal oee = tryParseDecimal(mapping.getColI());
                if (workers == null || workers <= 0 || ct == null || ct.compareTo(BigDecimal.ZERO) <= 0
                        || oee == null || oee.compareTo(BigDecimal.ZERO) <= 0) {
                    warningSet.add("产线-产品主线参数异常，line=" + lineCode + ", item=" + component + "（人数/CT/OEE）");
                    continue;
                }

                Product product = productById.get(new ProductId(component, lineCode));
                String productDescription = product == null ? null : product.getDescription();
                String pf = resolvePf(component, lineCode, productById, pfCacheByFamilyLine);
                BigDecimal oeeRatio = oee.divide(HUNDRED, 10, RoundingMode.HALF_UP);
                contexts.add(new ResolvedContext(lineCode, pf, productDescription, lineConfig, workers, ct, oee, oeeRatio));
            }

            if (contexts.isEmpty()) {
                warningSet.add("组件 [" + component + "] 在产线-产品主线映射中无可用产线");
            }
            contextsByComponent.put(component, contexts);
        }

        return new RequestCache(
                componentsByFinishedItem,
                contextsByComponent,
                lineClassByCode,
                new HashMap<>(),
                itemDescriptionByItemNumber);
    }

    private void processOneFinishedProductWeekly(
            String itemNumber,
            Map<String, BigDecimal> weeklyDemand,
            List<String> weeks,
            Map<String, LocalDate[]> weekDateRanges,
            Map<String, List<Map<String, Object>>> linesData,
            RequestCache cache) {

        List<String> components = cache.componentsByFinishedItem.getOrDefault(itemNumber, Collections.emptyList());
        for (String componentNumber : components) {
            List<ResolvedContext> contexts = cache.contextsByComponent.getOrDefault(componentNumber, Collections.emptyList());
            for (ResolvedContext context : contexts) {
                Map<String, Object> row = buildBaseRow(itemNumber, componentNumber, context, cache);
                BigDecimal weeklyCapacityBase = BigDecimal.valueOf(context.lineConfig.getWorkingDaysPerWeek())
                        .multiply(BigDecimal.valueOf(context.lineConfig.getShiftsPerDay()))
                        .multiply(context.lineConfig.getHoursPerShift())
                        .multiply(context.oeeRatio)
                        .multiply(SEC_PER_HOUR);

                for (String week : weeks) {
                    BigDecimal demand = weeklyDemand == null
                            ? BigDecimal.ZERO
                            : weeklyDemand.getOrDefault(week, BigDecimal.ZERO);

                    LocalDate periodDate = weekDateRanges.get(week)[0];
                    BigDecimal manpowerFactor = resolveManpowerFactor(cache, context.lineCode, periodDate);
                    if (demand.compareTo(BigDecimal.ZERO) == 0) {
                        row.put(week + "_demand", BigDecimal.ZERO);
                        row.put(week + "_loading", BigDecimal.ZERO);
                        row.put(week + "_manpowerFactor", manpowerFactor);
                        continue;
                    }

                    BigDecimal denominator = weeklyCapacityBase.multiply(manpowerFactor);

                    BigDecimal load = BigDecimal.ZERO;
                    if (denominator.compareTo(BigDecimal.ZERO) > 0) {
                        load = demand.multiply(context.ct).divide(denominator, 4, RoundingMode.HALF_UP);
                    }

                    row.put(week + "_demand", demand);
                    row.put(week + "_loading", load);
                    row.put(week + "_manpowerFactor", manpowerFactor);
                }

                linesData.computeIfAbsent(context.lineCode, k -> new ArrayList<>()).add(row);
            }
        }
    }

    private void processOneFinishedProductMonthly(
            String itemNumber,
            Map<String, BigDecimal> monthlyDemand,
            List<String> months,
            Map<String, Integer> monthDays,
            Map<String, List<Map<String, Object>>> linesData,
            RequestCache cache) {

        List<String> components = cache.componentsByFinishedItem.getOrDefault(itemNumber, Collections.emptyList());
        for (String componentNumber : components) {
            List<ResolvedContext> contexts = cache.contextsByComponent.getOrDefault(componentNumber, Collections.emptyList());
            for (ResolvedContext context : contexts) {
                Map<String, Object> row = buildBaseRow(itemNumber, componentNumber, context, cache);
                BigDecimal dailyCapacityBase = BigDecimal.valueOf(context.lineConfig.getShiftsPerDay())
                        .multiply(context.lineConfig.getHoursPerShift())
                        .multiply(context.oeeRatio)
                        .multiply(SEC_PER_HOUR);

                BigDecimal weeklyWorkdayRatio = BigDecimal.valueOf(context.lineConfig.getWorkingDaysPerWeek())
                        .divide(SEVEN, 10, RoundingMode.HALF_UP);

                for (String month : months) {
                    BigDecimal demand = monthlyDemand == null
                            ? BigDecimal.ZERO
                            : monthlyDemand.getOrDefault(month, BigDecimal.ZERO);

                    int daysInMonth = monthDays.get(month);
                    BigDecimal workingDaysPerMonth = BigDecimal.valueOf(daysInMonth).multiply(weeklyWorkdayRatio);
                    LocalDate periodDate = LocalDate.parse(month + "-01");
                    BigDecimal manpowerFactor = resolveManpowerFactor(cache, context.lineCode, periodDate);
                    if (demand.compareTo(BigDecimal.ZERO) == 0) {
                        row.put(month + "_demand", BigDecimal.ZERO);
                        row.put(month + "_loading", BigDecimal.ZERO);
                        row.put(month + "_manpowerFactor", manpowerFactor);
                        continue;
                    }

                    BigDecimal denominator = workingDaysPerMonth
                            .multiply(dailyCapacityBase)
                            .multiply(manpowerFactor);

                    BigDecimal load = BigDecimal.ZERO;
                    if (denominator.compareTo(BigDecimal.ZERO) > 0) {
                        load = demand.multiply(context.ct).divide(denominator, 4, RoundingMode.HALF_UP);
                    }

                    row.put(month + "_demand", demand);
                    row.put(month + "_loading", load);
                    row.put(month + "_manpowerFactor", manpowerFactor);
                }

                linesData.computeIfAbsent(context.lineCode, k -> new ArrayList<>()).add(row);
            }
        }
    }

    private String resolvePf(
            String componentNumber,
            String lineCode,
            Map<ProductId, Product> productById,
            Map<String, Optional<String>> pfCacheByFamilyLine) {

        Product product = productById.get(new ProductId(componentNumber, lineCode));
        if (product == null || product.getFamilyCode() == null || product.getFamilyCode().isBlank()) {
            return null;
        }

        String familyCode = product.getFamilyCode();
        String cacheKey = familyCode + "|" + lineCode;
        Optional<String> cached = pfCacheByFamilyLine.get(cacheKey);
        if (cached == null) {
            cached = productFamilyRepository.findByFamilyCodeAndLineCode(familyCode, lineCode).map(ProductFamily::getPf);
            pfCacheByFamilyLine.put(cacheKey, cached);
        }
        return cached.orElse(null);
    }

    private Integer tryParseInteger(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return new BigDecimal(value.trim()).intValue();
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal tryParseDecimal(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return new BigDecimal(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> buildBaseRow(String itemNumber, String componentNumber, ResolvedContext context, RequestCache cache) {
        BigDecimal shiftOutput = SEC_PER_HOUR
                .divide(context.ct, 10, RoundingMode.HALF_UP)
                .multiply(context.oeeRatio)
                .multiply(context.lineConfig.getHoursPerShift())
                .setScale(2, RoundingMode.HALF_UP);

        String itemDescription = cache.itemDescriptionByItemNumber.get(itemNumber);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("itemNumber", itemNumber);
        row.put("description", itemDescription == null ? "" : itemDescription);
        row.put("componentNumber", componentNumber);
        row.put("pf", context.pf);
        row.put("shiftOutput", shiftOutput);
        row.put("shiftWorkers", context.shiftWorkers);
        row.put("ct", context.ct);
        row.put("oee", context.oee);
        return row;
    }

    private BigDecimal resolveManpowerFactor(RequestCache cache, String lineCode, LocalDate date) {
        String key = lineCode + "|" + date;
        BigDecimal cached = cache.manpowerFactorByLineDate.get(key);
        if (cached != null) {
            return cached;
        }

        String lineClass = cache.lineClassByLineCode.get(lineCode);
        if (lineClass == null || lineClass.isBlank()) {
            lineClass = inferLineClass(lineCode);
        }

        BigDecimal factor = manpowerPlanService.resolveFactor(lineClass, date);
        cache.manpowerFactorByLineDate.put(key, factor);
        return factor;
    }

    private String inferLineClass(String lineCode) {
        if (lineCode == null || lineCode.length() < 3) {
            return "UNKNOWN";
        }
        return lineCode.substring(0, 3).toUpperCase();
    }

    private String buildResultCacheKey(String createdBy, String fileName, String version) {
        return (createdBy == null ? "" : createdBy) + "|" + (fileName == null ? "" : fileName) + "|" + (version == null ? "" : version);
    }

    private Map<String, Object> getCachedResult(ConcurrentHashMap<String, CacheEntry> cache, String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() - entry.cachedAtMs > RESULT_CACHE_TTL_MS) {
            cache.remove(key, entry);
            return null;
        }
        return entry.result;
    }

    private void putCachedResult(ConcurrentHashMap<String, CacheEntry> cache, String key, Map<String, Object> result) {
        if (cache.size() >= RESULT_CACHE_MAX_SIZE) {
            pruneExpiredEntries(cache);
            if (cache.size() >= RESULT_CACHE_MAX_SIZE) {
                String firstKey = cache.keys().hasMoreElements() ? cache.keys().nextElement() : null;
                if (firstKey != null) {
                    cache.remove(firstKey);
                }
            }
        }
        cache.put(key, new CacheEntry(result));
    }

    private void pruneExpiredEntries(ConcurrentHashMap<String, CacheEntry> cache) {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> now - entry.getValue().cachedAtMs > RESULT_CACHE_TTL_MS);
    }

    private static class RequestCache {
        private final Map<String, List<String>> componentsByFinishedItem;
        private final Map<String, List<ResolvedContext>> contextsByComponent;
        private final Map<String, String> lineClassByLineCode;
        private final Map<String, BigDecimal> manpowerFactorByLineDate;
        private final Map<String, String> itemDescriptionByItemNumber;

        private RequestCache(
                Map<String, List<String>> componentsByFinishedItem,
                Map<String, List<ResolvedContext>> contextsByComponent,
                Map<String, String> lineClassByLineCode,
                Map<String, BigDecimal> manpowerFactorByLineDate,
                Map<String, String> itemDescriptionByItemNumber) {
            this.componentsByFinishedItem = componentsByFinishedItem;
            this.contextsByComponent = contextsByComponent;
            this.lineClassByLineCode = lineClassByLineCode;
            this.manpowerFactorByLineDate = manpowerFactorByLineDate;
            this.itemDescriptionByItemNumber = itemDescriptionByItemNumber;
        }
    }

    private static class CacheEntry {
        private final Map<String, Object> result;
        private final long cachedAtMs;

        private CacheEntry(Map<String, Object> result) {
            this.result = result;
            this.cachedAtMs = System.currentTimeMillis();
        }
    }

    private static class ResolvedContext {
        private final String lineCode;
        private final String pf;
        private final String description;
        private final LineConfig lineConfig;
        private final Integer shiftWorkers;
        private final BigDecimal ct;
        private final BigDecimal oee;
        private final BigDecimal oeeRatio;

        private ResolvedContext(
                String lineCode,
                String pf,
                String description,
                LineConfig lineConfig,
                Integer shiftWorkers,
                BigDecimal ct,
                BigDecimal oee,
                BigDecimal oeeRatio) {
            this.lineCode = lineCode;
            this.pf = pf;
            this.description = description;
            this.lineConfig = lineConfig;
            this.shiftWorkers = shiftWorkers;
            this.ct = ct;
            this.oee = oee;
            this.oeeRatio = oeeRatio;
        }
    }
}

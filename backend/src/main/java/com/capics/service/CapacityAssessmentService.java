package com.capics.service;

import com.capics.entity.CtLineData;
import com.capics.entity.LineConfig;
import com.capics.entity.LineProfile;
import com.capics.entity.Product;
import com.capics.entity.ProductFamily;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class CapacityAssessmentService {

    private final MrpPlanService mrpPlanService;
    private final RoutingRepository routingRepository;
    private final RoutingItemRepository routingItemRepository;
    private final ProductRepository productRepository;
    private final ProductFamilyRepository productFamilyRepository;
    private final CtLineDataRepository ctLineDataRepository;
    private final LineConfigRepository lineConfigRepository;
    private final LineProfileRepository lineProfileRepository;
    private final ManpowerPlanService manpowerPlanService;

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
        List<String> warnings = new ArrayList<>();
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
            result.put("warnings", warnings);
            return result;
        }

        List<String> weeks = new ArrayList<>(weekDateRanges.keySet());
        Map<String, String> weekDates = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
        for (Map.Entry<String, LocalDate[]> entry : weekDateRanges.entrySet()) {
            weekDates.put(entry.getKey(), entry.getValue()[0].format(formatter));
        }

        Map<String, List<Map<String, Object>>> linesData = new LinkedHashMap<>();
        for (Map<String, Object> mrpItem : mrpItems) {
            String itemNumber = (String) mrpItem.get("itemNumber");
            String description = (String) mrpItem.getOrDefault("description", "");
            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> weeklyDemand = (Map<String, BigDecimal>) mrpItem.get("weeks");

            processOneFinishedProductWeekly(itemNumber, description, weeklyDemand, weeks, weekDateRanges, linesData, warnings);
        }

        result.put("lines", linesData);
        result.put("weeks", weeks);
        result.put("weekDates", weekDates);
        result.put("warnings", warnings);
        return result;
    }

    public Map<String, Object> getCapacityAssessmentMonthly(String createdBy, String fileName, String version) {
        List<String> warnings = new ArrayList<>();
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
            result.put("warnings", warnings);
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

        Map<String, List<Map<String, Object>>> linesData = new LinkedHashMap<>();
        for (Map<String, Object> mrpItem : mrpItems) {
            String itemNumber = (String) mrpItem.get("itemNumber");
            String description = (String) mrpItem.getOrDefault("description", "");
            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> monthlyDemand = (Map<String, BigDecimal>) mrpItem.get("months");

            processOneFinishedProductMonthly(itemNumber, description, monthlyDemand, months, monthDays, linesData, warnings);
        }

        result.put("lines", linesData);
        result.put("months", months);
        result.put("monthDates", monthDates);
        result.put("monthDays", monthDays);
        result.put("warnings", warnings);
        return result;
    }

    private void processOneFinishedProductWeekly(
            String itemNumber,
            String description,
            Map<String, BigDecimal> weeklyDemand,
            List<String> weeks,
            Map<String, LocalDate[]> weekDateRanges,
            Map<String, List<Map<String, Object>>> linesData,
            List<String> warnings) {

        List<RoutingItem> allRoutingItems = findRoutingItemsByFinishedItem(itemNumber, warnings);
        for (RoutingItem routingItem : allRoutingItems) {
            String componentNumber = routingItem.getComponentNumber();
            List<ResolvedContext> contexts = resolveContextsFromCt(componentNumber, warnings);
            for (ResolvedContext context : contexts) {
                Map<String, Object> row = buildBaseRow(itemNumber, description, componentNumber, context);
                BigDecimal oeeDecimal = context.oee.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                BigDecimal shiftsPerDay = BigDecimal.valueOf(context.lineConfig.getShiftsPerDay());
                BigDecimal workingDaysPerWeek = BigDecimal.valueOf(context.lineConfig.getWorkingDaysPerWeek());
                BigDecimal hoursPerShift = context.lineConfig.getHoursPerShift();
                BigDecimal ct = context.ct;

                for (String week : weeks) {
                    BigDecimal demand = weeklyDemand.getOrDefault(week, BigDecimal.ZERO);
                    LocalDate periodDate = weekDateRanges.get(week)[0];
                    BigDecimal manpowerFactor = resolveManpowerFactor(context.lineCode, periodDate);
                    BigDecimal denominator = workingDaysPerWeek
                            .multiply(shiftsPerDay)
                            .multiply(hoursPerShift)
                            .multiply(oeeDecimal)
                            .multiply(BigDecimal.valueOf(3600))
                            .multiply(manpowerFactor);

                    BigDecimal load = BigDecimal.ZERO;
                    if (denominator.compareTo(BigDecimal.ZERO) > 0) {
                        load = demand.multiply(ct).divide(denominator, 4, RoundingMode.HALF_UP);
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
            String description,
            Map<String, BigDecimal> monthlyDemand,
            List<String> months,
            Map<String, Integer> monthDays,
            Map<String, List<Map<String, Object>>> linesData,
            List<String> warnings) {

        List<RoutingItem> allRoutingItems = findRoutingItemsByFinishedItem(itemNumber, warnings);
        for (RoutingItem routingItem : allRoutingItems) {
            String componentNumber = routingItem.getComponentNumber();
            List<ResolvedContext> contexts = resolveContextsFromCt(componentNumber, warnings);
            for (ResolvedContext context : contexts) {
                Map<String, Object> row = buildBaseRow(itemNumber, description, componentNumber, context);
                BigDecimal oeeDecimal = context.oee.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                BigDecimal shiftsPerDay = BigDecimal.valueOf(context.lineConfig.getShiftsPerDay());
                BigDecimal hoursPerShift = context.lineConfig.getHoursPerShift();
                BigDecimal ct = context.ct;

                for (String month : months) {
                    BigDecimal demand = monthlyDemand.getOrDefault(month, BigDecimal.ZERO);
                    int daysInMonth = monthDays.get(month);
                    BigDecimal workingDaysPerMonth = BigDecimal.valueOf(daysInMonth)
                            .multiply(BigDecimal.valueOf(context.lineConfig.getWorkingDaysPerWeek()))
                            .divide(BigDecimal.valueOf(7), 10, RoundingMode.HALF_UP);
                    LocalDate periodDate = LocalDate.parse(month + "-01");
                    BigDecimal manpowerFactor = resolveManpowerFactor(context.lineCode, periodDate);

                    BigDecimal denominator = workingDaysPerMonth
                            .multiply(shiftsPerDay)
                            .multiply(hoursPerShift)
                            .multiply(oeeDecimal)
                            .multiply(BigDecimal.valueOf(3600))
                            .multiply(manpowerFactor);

                    BigDecimal load = BigDecimal.ZERO;
                    if (denominator.compareTo(BigDecimal.ZERO) > 0) {
                        load = demand.multiply(ct).divide(denominator, 4, RoundingMode.HALF_UP);
                    }

                    row.put(month + "_demand", demand);
                    row.put(month + "_loading", load);
                    row.put(month + "_manpowerFactor", manpowerFactor);
                }

                linesData.computeIfAbsent(context.lineCode, k -> new ArrayList<>()).add(row);
            }
        }
    }

    private List<RoutingItem> findRoutingItemsByFinishedItem(String itemNumber, List<String> warnings) {
        List<Routing> routings = routingRepository.findAllByProductNumber(itemNumber);
        if (routings.isEmpty()) {
            warnings.add("成品 [" + itemNumber + "] 在工艺路线中未找到");
            return Collections.emptyList();
        }

        Set<String> processedComponents = new HashSet<>();
        List<RoutingItem> allRoutingItems = new ArrayList<>();
        for (Routing routing : routings) {
            List<RoutingItem> items = routingItemRepository.findByRoutingId(routing.getId());
            for (RoutingItem item : items) {
                if (processedComponents.add(item.getComponentNumber())) {
                    allRoutingItems.add(item);
                }
            }
        }
        return allRoutingItems;
    }

    private List<ResolvedContext> resolveContextsFromCt(String componentNumber, List<String> warnings) {
        // 对应关系：主备线=主 + 物料号=componentNumber + 生产线=lineCode
        List<CtLineData> mappings = ctLineDataRepository.findByColDAndColCOrderByIdDesc("主", componentNumber);
        if (mappings.isEmpty()) {
            warnings.add("组件 [" + componentNumber + "] 在产线-产品表（主线）未找到对应关系");
            return Collections.emptyList();
        }

        Set<String> dedupLineCodes = new HashSet<>();
        List<ResolvedContext> contexts = new ArrayList<>();

        for (CtLineData mapping : mappings) {
            String lineCode = mapping.getColB() == null ? null : mapping.getColB().trim();
            if (lineCode == null || lineCode.isEmpty() || !dedupLineCodes.add(lineCode)) {
                continue;
            }

            Optional<LineConfig> lineConfigOpt = lineConfigRepository.findById(lineCode);
            if (lineConfigOpt.isEmpty() || !Boolean.TRUE.equals(lineConfigOpt.get().getIsActive())) {
                warnings.add("产线 [" + lineCode + "] 未启用，组件 [" + componentNumber + "] 已跳过");
                continue;
            }

            Integer workers = tryParseInteger(mapping.getColP());
            BigDecimal ct = tryParseDecimal(mapping.getColF());
            BigDecimal oee = tryParseDecimal(mapping.getColI());
            if (workers == null || workers <= 0 || ct == null || ct.compareTo(BigDecimal.ZERO) <= 0 || oee == null || oee.compareTo(BigDecimal.ZERO) <= 0) {
                warnings.add("产线-产品主线参数异常，line=" + lineCode + ", item=" + componentNumber + "（人数/CT/OEE）");
                continue;
            }

            String pf = resolvePf(componentNumber, lineCode);
            contexts.add(new ResolvedContext(lineCode, pf, lineConfigOpt.get(), workers, ct, oee));
        }

        if (contexts.isEmpty()) {
            warnings.add("组件 [" + componentNumber + "] 在产线-产品主线映射中无可用产线");
        }
        return contexts;
    }

    private String resolvePf(String componentNumber, String lineCode) {
        return productRepository.findById(new com.capics.entity.ProductId(componentNumber, lineCode))
                .map(Product::getFamilyCode)
                .flatMap(familyCode -> productFamilyRepository.findByFamilyCodeAndLineCode(familyCode, lineCode).map(ProductFamily::getPf))
                .orElse(null);
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

    private Map<String, Object> buildBaseRow(
            String itemNumber,
            String description,
            String componentNumber,
            ResolvedContext context) {

        BigDecimal shiftOutput = BigDecimal.valueOf(3600)
                .divide(context.ct, 10, RoundingMode.HALF_UP)
                .multiply(context.oee.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                .multiply(context.lineConfig.getHoursPerShift())
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("itemNumber", itemNumber);
        // Description 统一取产品主数据中的 PF
        row.put("description", context.pf == null ? "" : context.pf);
        row.put("componentNumber", componentNumber);
        row.put("pf", context.pf);
        row.put("shiftOutput", shiftOutput);
        row.put("shiftWorkers", context.shiftWorkers);
        row.put("ct", context.ct);
        row.put("oee", context.oee);
        return row;
    }

    private BigDecimal resolveManpowerFactor(String lineCode, LocalDate date) {
        String lineClass = lineProfileRepository.findById(lineCode)
                .map(LineProfile::getLineClass)
                .orElseGet(() -> inferLineClass(lineCode));
        return manpowerPlanService.resolveFactor(lineClass, date);
    }

    private String inferLineClass(String lineCode) {
        if (lineCode == null || lineCode.length() < 3) {
            return "UNKNOWN";
        }
        return lineCode.substring(0, 3).toUpperCase();
    }

    private static class ResolvedContext {
        private final String lineCode;
        private final String pf;
        private final LineConfig lineConfig;
        private final Integer shiftWorkers;
        private final BigDecimal ct;
        private final BigDecimal oee;

        private ResolvedContext(
                String lineCode,
                String pf,
                LineConfig lineConfig,
                Integer shiftWorkers,
                BigDecimal ct,
                BigDecimal oee) {
            this.lineCode = lineCode;
            this.pf = pf;
            this.lineConfig = lineConfig;
            this.shiftWorkers = shiftWorkers;
            this.ct = ct;
            this.oee = oee;
        }
    }
}

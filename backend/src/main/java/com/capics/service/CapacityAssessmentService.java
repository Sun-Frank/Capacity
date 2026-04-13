package com.capics.service;

import com.capics.entity.*;
import com.capics.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CapacityAssessmentService {

    private final MrpPlanService mrpPlanService;
    private final RoutingRepository routingRepository;
    private final RoutingItemRepository routingItemRepository;
    private final ProductRepository productRepository;
    private final ProductFamilyRepository productFamilyRepository;
    private final FamilyLineRepository familyLineRepository;
    private final LineConfigRepository lineConfigRepository;
    private final LineProfileRepository lineProfileRepository;
    private final ManpowerPlanService manpowerPlanService;

    public CapacityAssessmentService(
            MrpPlanService mrpPlanService,
            RoutingRepository routingRepository,
            RoutingItemRepository routingItemRepository,
            ProductRepository productRepository,
            ProductFamilyRepository productFamilyRepository,
            FamilyLineRepository familyLineRepository,
            LineConfigRepository lineConfigRepository,
            LineProfileRepository lineProfileRepository,
            ManpowerPlanService manpowerPlanService) {
        this.mrpPlanService = mrpPlanService;
        this.routingRepository = routingRepository;
        this.routingItemRepository = routingItemRepository;
        this.productRepository = productRepository;
        this.productFamilyRepository = productFamilyRepository;
        this.familyLineRepository = familyLineRepository;
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
            Optional<ResolvedContext> contextOpt = resolveContext(componentNumber, warnings);
            if (contextOpt.isEmpty()) {
                continue;
            }
            ResolvedContext context = contextOpt.get();

            Map<String, Object> row = buildBaseRow(itemNumber, description, componentNumber, context);
            BigDecimal oeeDecimal = context.product.getOee().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
            BigDecimal shiftsPerDay = BigDecimal.valueOf(context.lineConfig.getShiftsPerDay());
            BigDecimal workingDaysPerWeek = BigDecimal.valueOf(context.lineConfig.getWorkingDaysPerWeek());
            BigDecimal hoursPerShift = context.lineConfig.getHoursPerShift();
            BigDecimal ct = context.product.getCycleTime();
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
            Optional<ResolvedContext> contextOpt = resolveContext(componentNumber, warnings);
            if (contextOpt.isEmpty()) {
                continue;
            }
            ResolvedContext context = contextOpt.get();

            Map<String, Object> row = buildBaseRow(itemNumber, description, componentNumber, context);
            BigDecimal oeeDecimal = context.product.getOee().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
            BigDecimal shiftsPerDay = BigDecimal.valueOf(context.lineConfig.getShiftsPerDay());
            BigDecimal hoursPerShift = context.lineConfig.getHoursPerShift();
            BigDecimal ct = context.product.getCycleTime();

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

    private List<RoutingItem> findRoutingItemsByFinishedItem(String itemNumber, List<String> warnings) {
        List<Routing> routings = routingRepository.findAllByProductNumber(itemNumber);
        if (routings.isEmpty()) {
            warnings.add("成品 [" + itemNumber + "] 在工艺路线表中未找到");
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

    private Optional<ResolvedContext> resolveContext(String componentNumber, List<String> warnings) {
        List<Product> componentProducts = productRepository.findByItemNumber(componentNumber);
        if (componentProducts.isEmpty()) {
            warnings.add("组件 [" + componentNumber + "] 在产品表中未找到");
            return Optional.empty();
        }

        String familyCode = componentProducts.get(0).getFamilyCode();
        if (familyCode == null || familyCode.isEmpty()) {
            warnings.add("组件 [" + componentNumber + "] 未设置编码族");
            return Optional.empty();
        }

        String lineCode = null;
        for (FamilyLine fl : familyLineRepository.findAll()) {
            if (fl.getFamilyCode().equals(familyCode)) {
                lineCode = fl.getLineCode();
                break;
            }
        }
        if (lineCode == null) {
            warnings.add("编码族 [" + familyCode + "] 未找到定线信息");
            return Optional.empty();
        }

        String pf = productFamilyRepository.findByFamilyCodeAndLineCode(familyCode, lineCode)
                .map(ProductFamily::getPf)
                .orElse(null);

        Optional<LineConfig> lineConfigOpt = lineConfigRepository.findById(lineCode);
        if (lineConfigOpt.isEmpty() || !Boolean.TRUE.equals(lineConfigOpt.get().getIsActive())) {
            warnings.add("产线 [" + lineCode + "] 未激活");
            return Optional.empty();
        }
        LineConfig lineConfig = lineConfigOpt.get();

        Product componentOnLine = null;
        for (Product p : componentProducts) {
            if (lineCode.equals(p.getLineCode())) {
                componentOnLine = p;
                break;
            }
        }
        if (componentOnLine == null) {
            warnings.add("组件 [" + componentNumber + "] 在产线 [" + lineCode + "] 下无产品信息");
            return Optional.empty();
        }

        BigDecimal ct = componentOnLine.getCycleTime();
        BigDecimal oee = componentOnLine.getOee();
        Integer workerCount = componentOnLine.getWorkerCount();
        if (ct == null || oee == null || workerCount == null || ct.compareTo(BigDecimal.ZERO) <= 0) {
            warnings.add("组件 [" + componentNumber + "] 在产线 [" + lineCode + "] 的CT/OEE/人力参数不完整");
            return Optional.empty();
        }

        return Optional.of(new ResolvedContext(lineCode, pf, componentOnLine, lineConfig));
    }

    private Map<String, Object> buildBaseRow(
            String itemNumber,
            String description,
            String componentNumber,
            ResolvedContext context) {

        BigDecimal shiftOutput = BigDecimal.valueOf(3600)
                .divide(context.product.getCycleTime(), 10, RoundingMode.HALF_UP)
                .multiply(context.product.getOee().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                .multiply(context.lineConfig.getHoursPerShift())
                .setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("itemNumber", itemNumber);
        row.put("description", description);
        row.put("componentNumber", componentNumber);
        row.put("pf", context.pf);
        row.put("shiftOutput", shiftOutput);
        row.put("shiftWorkers", context.product.getWorkerCount());
        row.put("ct", context.product.getCycleTime());
        row.put("oee", context.product.getOee());
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
        private final Product product;
        private final LineConfig lineConfig;

        private ResolvedContext(String lineCode, String pf, Product product, LineConfig lineConfig) {
            this.lineCode = lineCode;
            this.pf = pf;
            this.product = product;
            this.lineConfig = lineConfig;
        }
    }
}

package com.capics.service;

import com.capics.dto.CapacityAssessmentDto;
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

    public CapacityAssessmentService(
            MrpPlanService mrpPlanService,
            RoutingRepository routingRepository,
            RoutingItemRepository routingItemRepository,
            ProductRepository productRepository,
            ProductFamilyRepository productFamilyRepository,
            FamilyLineRepository familyLineRepository,
            LineConfigRepository lineConfigRepository) {
        this.mrpPlanService = mrpPlanService;
        this.routingRepository = routingRepository;
        this.routingItemRepository = routingItemRepository;
        this.productRepository = productRepository;
        this.productFamilyRepository = productFamilyRepository;
        this.familyLineRepository = familyLineRepository;
        this.lineConfigRepository = lineConfigRepository;
    }

    /**
     * 获取产能评估数据
     * 数据流：MRP成品 → Routing(组件) → Product(家族) → FamilyLine(产线) → Product(CT/OEE/人)
     *
     * 返回格式：
     * {
     *   lines: { lineCode: [ { itemNumber, description, componentNumber, shiftOutput, shiftWorkers, ct, oee, W01_demand, W01_loading, ... } ] },
     *   weeks: ["W01", "W02", ...],
     *   weekDates: { "W01": "2026/3/2", "W02": "2026/3/9", ... },
     *   warnings: ["警告1", "警告2", ...]
     * }
     */
    public Map<String, Object> getCapacityAssessment(String createdBy, String fileName, String version) {
        List<String> warnings = new ArrayList<>();
        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 获取MRP计划的周需求量
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

        // 2. 构建weeks列表和weekDates映射（用于前端显示）
        List<String> weeks = new ArrayList<>(weekDateRanges.keySet());
        Map<String, String> weekDates = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
        for (Map.Entry<String, LocalDate[]> entry : weekDateRanges.entrySet()) {
            weekDates.put(entry.getKey(), entry.getValue()[0].format(formatter));
        }

        // 3. 按产线分组的结果（每行一个Map，展平的周数据）
        Map<String, List<Map<String, Object>>> linesData = new LinkedHashMap<>();

        // 4. 遍历每个MRP成品
        for (Map<String, Object> mrpItem : mrpItems) {
            String itemNumber = (String) mrpItem.get("itemNumber");
            String description = (String) mrpItem.getOrDefault("description", "");
            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> weeklyDemand = (Map<String, BigDecimal>) mrpItem.get("weeks");

            // 查找工艺路线（可能有多条，用Set避免重复组件）
            List<Routing> routings = routingRepository.findAllByProductNumber(itemNumber);
            if (routings.isEmpty()) {
                warnings.add("成品 [" + itemNumber + "] 在工艺路线表中未找到");
                continue;
            }

            Set<String> processedComponents = new HashSet<>();
            List<RoutingItem> allRoutingItems = new ArrayList<>();
            for (Routing routing : routings) {
                List<RoutingItem> items = routingItemRepository.findByRoutingId(routing.getId());
                for (RoutingItem item : items) {
                    if (!processedComponents.contains(item.getComponentNumber())) {
                        processedComponents.add(item.getComponentNumber());
                        allRoutingItems.add(item);
                    }
                }
            }

            // 5. 遍历每个组件
            for (RoutingItem routingItem : allRoutingItems) {
                String componentNumber = routingItem.getComponentNumber();

                // 6. 查找组件在产品表中的信息（获取familyCode）
                List<Product> componentProducts = productRepository.findByItemNumber(componentNumber);
                if (componentProducts.isEmpty()) {
                    warnings.add("组件 [" + componentNumber + "] 在产品列表中未找到");
                    continue;
                }

                // 验证所有记录的familyCode一致
                String familyCode = componentProducts.get(0).getFamilyCode();
                if (familyCode == null || familyCode.isEmpty()) {
                    warnings.add("组件 [" + componentNumber + "] 未设置编码族");
                    continue;
                }

                // 7. 通过familyCode查找定线信息，获取lineCode
                String lineCode = null;
                for (FamilyLine fl : familyLineRepository.findAll()) {
                    if (fl.getFamilyCode().equals(familyCode)) {
                        lineCode = fl.getLineCode();
                        break;
                    }
                }

                if (lineCode == null) {
                    warnings.add("编码族 [" + familyCode + "] 未找到定线信息");
                    continue;
                }

                // 8. 检查产线是否激活
                Optional<LineConfig> lineConfigOpt = lineConfigRepository.findById(lineCode);
                if (lineConfigOpt.isEmpty() || !lineConfigOpt.get().getIsActive()) {
                    warnings.add("产线 [" + lineCode + "] 未激活");
                    continue;
                }

                LineConfig lineConfig = lineConfigOpt.get();

                // 9. 获取组件在该产线的产品信息（CT、OEE、班人数）
                Product product = null;
                for (Product p : componentProducts) {
                    if (p.getLineCode().equals(lineCode)) {
                        product = p;
                        break;
                    }
                }

                if (product == null) {
                    warnings.add("组件 [" + componentNumber + "] 在产线 [" + lineCode + "] 下无产品信息");
                    continue;
                }

                BigDecimal ct = product.getCycleTime();
                BigDecimal oee = product.getOee();
                Integer workerCount = product.getWorkerCount();

                if (ct == null || oee == null || workerCount == null || ct.compareTo(BigDecimal.ZERO) <= 0) {
                    warnings.add("组件 [" + componentNumber + "] 在产线 [" + lineCode + "] 的CT/OEE/班人数不完整");
                    continue;
                }

                // 10. 计算班产量
                // 班产量 = (3600 / CT) × (OEE / 100) × hoursPerShift
                BigDecimal hoursPerShift = lineConfig.getHoursPerShift();
                BigDecimal shiftOutput = BigDecimal.valueOf(3600)
                        .divide(ct, 10, RoundingMode.HALF_UP)
                        .multiply(oee.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                        .multiply(hoursPerShift)
                        .setScale(2, RoundingMode.HALF_UP);

                // 11. 计算每周的LOAD和需求量
                // LOAD = (需求量 × CT) / (工作天数 × 每天班数 × 每班工作时长(h) × OEE/100 × 3600)
                // OEE在数据库中存的是85表示85%，需要除以100转换为小数
                BigDecimal oeeDecimal = oee.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                BigDecimal workingDaysPerWeek = BigDecimal.valueOf(lineConfig.getWorkingDaysPerWeek());
                BigDecimal shiftsPerDay = BigDecimal.valueOf(lineConfig.getShiftsPerDay());
                BigDecimal denominator = workingDaysPerWeek
                        .multiply(shiftsPerDay)
                        .multiply(hoursPerShift)
                        .multiply(oeeDecimal)
                        .multiply(BigDecimal.valueOf(3600));

                System.out.println("产线=" + lineCode + ", 分母=" + denominator + ", shiftOutput=" + shiftOutput);

                // 构建展平的周数据行
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("itemNumber", itemNumber);
                row.put("description", description);
                row.put("componentNumber", componentNumber);
                row.put("shiftOutput", shiftOutput);
                row.put("shiftWorkers", workerCount);
                row.put("ct", ct);
                row.put("oee", oee);

                for (String week : weeks) {
                    BigDecimal demand = weeklyDemand.get(week);
                    if (demand == null) {
                        demand = BigDecimal.ZERO;
                    }

                    BigDecimal load = BigDecimal.ZERO;
                    if (denominator.compareTo(BigDecimal.ZERO) > 0) {
                        load = demand.multiply(ct).divide(denominator, 4, RoundingMode.HALF_UP);
                    }

                    row.put(week + "_demand", demand);
                    row.put(week + "_loading", load);
                }

                // 12. 按产线分组
                linesData.computeIfAbsent(lineCode, k -> new ArrayList<>()).add(row);
            }
        }

        result.put("lines", linesData);
        result.put("weeks", weeks);
        result.put("weekDates", weekDates);
        result.put("warnings", warnings);

        return result;
    }
}

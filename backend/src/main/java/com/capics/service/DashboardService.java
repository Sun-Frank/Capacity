package com.capics.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class DashboardService {

    private final CapacityAssessmentService capacityAssessmentService;

    public DashboardService(CapacityAssessmentService capacityAssessmentService) {
        this.capacityAssessmentService = capacityAssessmentService;
    }

    /**
     * 获取产线负载矩阵
     * type: static (静态产能核算) 或 dynamic (动态产能模拟)
     * dimension: week 或 month
     */
    public Map<String, Object> getLoadingMatrix(String type, String dimension,
                                                 String createdBy, String fileName, String version) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        // 如果没有传入筛选条件，使用默认值
        if (createdBy == null || createdBy.isEmpty()) {
            createdBy = "";
        }
        if (fileName == null || fileName.isEmpty()) {
            fileName = "";
        }
        if (version == null || version.isEmpty()) {
            version = "";
        }

        // 调用对应的产能评估服务
        Map<String, Object> capacityData;
        if ("dynamic".equals(type) && "month".equals(dimension)) {
            capacityData = capacityAssessmentService.getCapacityAssessmentMonthly(createdBy, fileName, version);
        } else {
            // 默认使用周维度静态产能评估
            capacityData = capacityAssessmentService.getCapacityAssessment(createdBy, fileName, version);
        }

        // 提取日期列表
        List<String> dates;
        Map<String, String> dateLabels;

        if ("month".equals(dimension)) {
            dates = (List<String>) capacityData.getOrDefault("months", new ArrayList<>());
            dateLabels = (Map<String, String>) capacityData.getOrDefault("monthDates", new LinkedHashMap<>());
        } else {
            dates = (List<String>) capacityData.getOrDefault("weeks", new ArrayList<>());
            dateLabels = (Map<String, String>) capacityData.getOrDefault("weekDates", new LinkedHashMap<>());
        }

        // 提取产线数据
        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, Object>>> linesData =
                (Map<String, List<Map<String, Object>>>) capacityData.getOrDefault("lines", new LinkedHashMap<>());

        // 构建行数据
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, List<String>> groupLines = new LinkedHashMap<>(); // group -> lineCodes
        Map<String, List<Double>> groupLoadings = new LinkedHashMap<>(); // group -> [loading values]

        // 按产线遍历
        for (Map.Entry<String, List<Map<String, Object>>> entry : linesData.entrySet()) {
            String lineCode = entry.getKey();
            List<Map<String, Object>> items = entry.getValue();

            // 判断是否是 SMT 开头的产线
            String group = lineCode.startsWith("SMT") ? "SMT" : null;

            // 计算该产线在每个时间点的总 LOAD（该产线可能有多个组件）
            List<Double> lineLoadings = new ArrayList<>();
            for (String date : dates) {
                double totalLoad = 0;
                for (Map<String, Object> item : items) {
                    Object loadObj = item.get(date + "_loading");
                    if (loadObj != null) {
                        double load = 0;
                        if (loadObj instanceof BigDecimal) {
                            load = ((BigDecimal) loadObj).doubleValue();
                        } else if (loadObj instanceof Number) {
                            load = ((Number) loadObj).doubleValue();
                        }
                        totalLoad += load;
                    }
                }
                lineLoadings.add(totalLoad);
            }

            // 添加单盛行
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("lineCode", lineCode);
            row.put("loadings", lineLoadings);
            row.put("isTotal", false);
            row.put("group", group);
            row.put("threshold", 1.0); // 单条产线阈值 100%
            rows.add(row);

            // 如果是 SMT 组，累加到组
            if (group != null) {
                if (!groupLines.containsKey(group)) {
                    groupLines.put(group, new ArrayList<>());
                    groupLoadings.put(group, new ArrayList<>(Collections.nCopies(dates.size(), 0.0)));
                }
                groupLines.get(group).add(lineCode);
                List<Double> accumulated = groupLoadings.get(group);
                for (int i = 0; i < lineLoadings.size(); i++) {
                    accumulated.set(i, accumulated.get(i) + lineLoadings.get(i));
                }
            }
        }

        // 添加 SMT TOTAL 行
        for (Map.Entry<String, List<Double>> entry : groupLoadings.entrySet()) {
            String group = entry.getKey();
            List<Double> totalLoadings = entry.getValue();
            int lineCount = groupLines.get(group).size();
            double threshold = lineCount * 1.0;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("lineCode", group + " TOTAL");
            row.put("loadings", totalLoadings);
            row.put("isTotal", true);
            row.put("group", group);
            row.put("threshold", threshold);
            row.put("lineCount", lineCount);
            rows.add(row);

            // 检查预警
            for (int i = 0; i < totalLoadings.size(); i++) {
                if (totalLoadings.get(i) > threshold) {
                    warnings.add(group + " TOTAL 在 " + dates.get(i) + " 负载超过 " + (int)(threshold * 100) + "%");
                }
            }
        }

        result.put("source", type);
        result.put("dimension", dimension);
        result.put("dates", dates);
        result.put("dateLabels", dateLabels);
        // 修改后的排序逻辑
        rows.sort((a, b) -> {
            String codeA = (String) a.get("lineCode");
            String codeB = (String) b.get("lineCode");
            String groupA = (String) a.get("group");
            String groupB = (String) b.get("group");
            Boolean isTotalA = (Boolean) a.get("isTotal");
            Boolean isTotalB = (Boolean) b.get("isTotal");

            // 1. 先处理组的顺序 (如果不属于任何组，设为 lineCode 本身用于比较)
            String compGroupA = groupA != null ? groupA : codeA;
            String compGroupB = groupB != null ? groupB : codeB;

            // 按组名降序排列 (SMT 会排在 ICP 和 FA 前面)
            int groupCompare = -compGroupA.compareTo(compGroupB);

            if (groupCompare != 0) {
                return groupCompare;
            }

            // 2. 如果在同一个组内（比如都是 SMT）
            // TOTAL 行永远排在最后
            if (isTotalA && !isTotalB) return 1;
            if (!isTotalA && isTotalB) return -1;

            // 3. 同组内的普通行，按 lineCode 降序排
            return -codeA.compareTo(codeB);
        });
        result.put("rows", rows);
        result.put("warnings", warnings);

        return result;
    }
}

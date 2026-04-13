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

    public Map<String, Object> getLoadingMatrix(String type, String dimension,
                                                 String createdBy, String fileName, String version) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        if (createdBy == null) createdBy = "";
        if (fileName == null) fileName = "";
        if (version == null) version = "";

        Map<String, Object> capacityData;
        if ("month".equals(dimension)) {
            capacityData = capacityAssessmentService.getCapacityAssessmentMonthly(createdBy, fileName, version);
        } else {
            capacityData = capacityAssessmentService.getCapacityAssessment(createdBy, fileName, version);
        }

        List<String> dates;
        Map<String, String> dateLabels;
        if ("month".equals(dimension)) {
            dates = (List<String>) capacityData.getOrDefault("months", new ArrayList<>());
            dateLabels = (Map<String, String>) capacityData.getOrDefault("monthDates", new LinkedHashMap<>());
        } else {
            dates = (List<String>) capacityData.getOrDefault("weeks", new ArrayList<>());
            dateLabels = (Map<String, String>) capacityData.getOrDefault("weekDates", new LinkedHashMap<>());
        }

        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, Object>>> linesData =
                (Map<String, List<Map<String, Object>>>) capacityData.getOrDefault("lines", new LinkedHashMap<>());

        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, List<String>> groupLines = new LinkedHashMap<>();
        Map<String, List<Double>> groupLoadings = new LinkedHashMap<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : linesData.entrySet()) {
            String lineCode = entry.getKey();
            List<Map<String, Object>> items = entry.getValue();

            String group = lineCode.startsWith("SMT") ? "SMT" : null;

            List<Double> lineLoadings = new ArrayList<>();
            for (String date : dates) {
                double totalLoad = 0;
                for (Map<String, Object> item : items) {
                    Object loadObj = item.get(date + "_loading");
                    if (loadObj instanceof BigDecimal) {
                        totalLoad += ((BigDecimal) loadObj).doubleValue();
                    } else if (loadObj instanceof Number) {
                        totalLoad += ((Number) loadObj).doubleValue();
                    }
                }
                lineLoadings.add(totalLoad);
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("lineCode", lineCode);
            row.put("loadings", lineLoadings);
            row.put("isTotal", false);
            row.put("group", group);
            row.put("threshold", 1.0);
            rows.add(row);

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

            for (int i = 0; i < totalLoadings.size(); i++) {
                if (totalLoadings.get(i) > threshold) {
                    warnings.add(group + " TOTAL 在" + dates.get(i) + " 负载超过 " + (int) (threshold * 100) + "%");
                }
            }
        }

        rows.sort((a, b) -> {
            String codeA = (String) a.get("lineCode");
            String codeB = (String) b.get("lineCode");
            String groupA = (String) a.get("group");
            String groupB = (String) b.get("group");
            Boolean isTotalA = (Boolean) a.get("isTotal");
            Boolean isTotalB = (Boolean) b.get("isTotal");

            String compGroupA = groupA != null ? groupA : codeA;
            String compGroupB = groupB != null ? groupB : codeB;

            int groupCompare = -compGroupA.compareTo(compGroupB);
            if (groupCompare != 0) {
                return groupCompare;
            }

            if (isTotalA && !isTotalB) return 1;
            if (!isTotalA && isTotalB) return -1;

            return -codeA.compareTo(codeB);
        });

        result.put("source", type);
        result.put("dimension", dimension);
        result.put("dates", dates);
        result.put("dateLabels", dateLabels);
        result.put("rows", rows);
        result.put("warnings", warnings);
        return result;
    }
}

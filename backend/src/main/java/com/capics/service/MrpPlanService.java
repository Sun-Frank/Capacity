package com.capics.service;

import com.capics.dto.MrpPlanDto;
import com.capics.entity.MrpPlan;
import com.capics.repository.MrpPlanRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MrpPlanService {

    private final MrpPlanRepository repository;

    public MrpPlanService(MrpPlanRepository repository) {
        this.repository = repository;
    }

    public List<MrpPlanDto> findAll() {
        return repository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<MrpPlanDto> findByVersion(String version) {
        return repository.findByVersion(version).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<String> getAllVersions() {
        return repository.findAllVersions();
    }

    public MrpPlanDto findById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("MRP Plan not found"));
    }

    public MrpPlanDto save(MrpPlanDto dto) {
        MrpPlan entity = toEntity(dto);
        entity = repository.save(entity);
        return toDto(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    // 获取最新导入的MRP文件信息
    public Map<String, String> getLatestMrpFileInfo() {
        List<MrpPlan> plans = repository.findLatestFileInfo();
        Map<String, String> result = new LinkedHashMap<>();
        if (plans != null && !plans.isEmpty()) {
            MrpPlan latest = plans.get(0);
            result.put("fileName", latest.getFileName());
            result.put("createdBy", latest.getCreatedBy());
        }
        return result;
    }

    public int importFromExcel(MultipartFile file, String fileName, String createdBy) throws IOException {
        // Check if this file already exists for this user
        if (repository.existsByCreatedByAndFileName(createdBy, fileName)) {
            throw new IOException("文件 '" + fileName + "' 已存在，请使用其他名称或删除现有文件后再试");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int count = 0;

            // Excel列索引对应:
            // A(0): Item Number, B(1): Description, C(2): Site, D(3): Production Line
            // E(4): Release, F(5): Due Date, G(6): Quantity Scheduled, H(7): Quantity Completed
            // I(8): Routing Code, J(9): 版本

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                MrpPlan entity = new MrpPlan();
                entity.setItemNumber(getCellValueAsString(row.getCell(0)));
                entity.setDescription(getCellValueAsString(row.getCell(1)));
                entity.setSite(getCellValueAsString(row.getCell(2)));
                entity.setProductionLine(getCellValueAsString(row.getCell(3)));

                Cell dateCell = row.getCell(4);
                if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC) {
                    Date date = DateUtil.getJavaDate(dateCell.getNumericCellValue());
                    entity.setReleaseDate(date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
                }

                Cell dueDateCell = row.getCell(5);
                if (dueDateCell != null && dueDateCell.getCellType() == CellType.NUMERIC) {
                    Date date = DateUtil.getJavaDate(dueDateCell.getNumericCellValue());
                    entity.setDueDate(date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
                }

                Cell qtyScheduledCell = row.getCell(6);
                if (qtyScheduledCell != null && qtyScheduledCell.getCellType() == CellType.NUMERIC) {
                    entity.setQuantityScheduled(BigDecimal.valueOf(qtyScheduledCell.getNumericCellValue()));
                }

                Cell qtyCompletedCell = row.getCell(7);
                if (qtyCompletedCell != null && qtyCompletedCell.getCellType() == CellType.NUMERIC) {
                    entity.setQuantityCompleted(BigDecimal.valueOf(qtyCompletedCell.getNumericCellValue()));
                }

                entity.setRoutingCode(getCellValueAsString(row.getCell(8)));
                entity.setVersion(getCellValueAsString(row.getCell(9)));
                entity.setFileName(fileName);
                entity.setCreatedBy(createdBy);

                if (entity.getItemNumber() != null && !entity.getItemNumber().isEmpty()
                        && entity.getVersion() != null && !entity.getVersion().isEmpty()) {
                    repository.save(entity);
                    count++;
                }
            }

            return count;
        }
    }

    public List<Map<String, Object>> getWeeklyReport(String version) {
        List<MrpPlan> plans = repository.findByVersionOrderByItemNumberAndReleaseDate(version);

        Map<String, Map<String, BigDecimal>> report = new LinkedHashMap<>();

        for (MrpPlan plan : plans) {
            String itemKey = plan.getItemNumber();
            report.putIfAbsent(itemKey, new LinkedHashMap<>());
            Map<String, BigDecimal> itemData = report.get(itemKey);

            int weekOfYear = plan.getReleaseDate().get(java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).weekOfYear());
            String weekKey = String.format("%02d", weekOfYear);

            BigDecimal currentQty = itemData.getOrDefault(weekKey, BigDecimal.ZERO);
            itemData.put(weekKey, currentQty.add(plan.getQuantityScheduled()));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : report.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemNumber", entry.getKey());
            row.put("weeklyDemand", entry.getValue());
            result.add(row);
        }

        return result;
    }

    public List<Map<String, Object>> getMonthlyReport(String version) {
        List<MrpPlan> plans = repository.findByVersionOrderByItemNumberAndReleaseDate(version);

        Map<String, Map<String, BigDecimal>> report = new LinkedHashMap<>();

        for (MrpPlan plan : plans) {
            String itemKey = plan.getItemNumber();
            report.putIfAbsent(itemKey, new LinkedHashMap<>());
            Map<String, BigDecimal> itemData = report.get(itemKey);

            String monthKey = String.format("%d月", plan.getReleaseDate().getMonthValue());

            BigDecimal currentQty = itemData.getOrDefault(monthKey, BigDecimal.ZERO);
            itemData.put(monthKey, currentQty.add(plan.getQuantityScheduled()));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : report.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemNumber", entry.getKey());
            row.put("monthlyDemand", entry.getValue());
            result.add(row);
        }

        return result;
    }

    // New filter methods for three-level cascade filtering
    public List<String> getAllCreatedBys() {
        return repository.findAllCreatedBys();
    }

    public List<String> getFileNamesByCreatedBy(String createdBy) {
        return repository.findFileNamesByCreatedBy(createdBy);
    }

    public List<String> getVersionsByCreatedByAndFileName(String createdBy, String fileName) {
        return repository.findVersionsByCreatedByAndFileName(createdBy, fileName);
    }

    public List<MrpPlanDto> findByCreatedByAndFileNameAndVersion(String createdBy, String fileName, String version) {
        return repository.findByCreatedByAndFileNameAndVersion(createdBy, fileName, version).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<MrpPlanDto> findByCreatedByAndFileName(String createdBy, String fileName) {
        return repository.findByCreatedByAndFileName(createdBy, fileName).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // 周报表 - 按导入人+文件名，获取所有版本对比数据
    // 格式：Item Number | Description | W10-2026(周一-周日) | W11-2026(周一-周日) | ...
    public List<Map<String, Object>> getWeeklyReportByCreatedByAndFileName(String createdBy, String fileName) {
        List<MrpPlan> plans = repository.findByCreatedByAndFileNameOrderByItemNumberAndReleaseDate(createdBy, fileName);

        // 获取所有版本
        List<String> allVersions = plans.stream()
                .map(MrpPlan::getVersion)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // 使用ISO标准周（周一到周日）
        java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;

        // 获取所有周及其标准日期范围（周一到周日），使用年份+周作为key保证排序正确
        Map<String, LocalDate[]> weekDateRanges = new TreeMap<>(Comparator.naturalOrder());

        for (MrpPlan plan : plans) {
            LocalDate date = plan.getReleaseDate();
            // 使用ISO周计算
            int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
            int year = date.get(weekFields.weekBasedYear());
            // 使用年份+周作为key，如 "2026W10"，保证跨年排序正确
            String weekKey = String.format("%dW%02d", year, weekOfYear);

            if (!weekDateRanges.containsKey(weekKey)) {
                // 计算该周的周一和周日
                LocalDate monday = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                LocalDate sunday = monday.plusDays(6);
                weekDateRanges.put(weekKey, new LocalDate[]{monday, sunday});
            }
        }

        // 生成列名：按年-周顺序排列
        List<Map<String, String>> columns = new ArrayList<>();
        for (String week : weekDateRanges.keySet()) {
            LocalDate[] range = weekDateRanges.get(week);
            // 提取年份和周数字用于显示，如 "2026W10(3/2-3/8)"
            String weekLabel = week + "(" + range[0].getMonthValue() + "/" + range[0].getDayOfMonth() + "-" + range[1].getMonthValue() + "/" + range[1].getDayOfMonth() + ")";
            for (String version : allVersions) {
                Map<String, String> col = new LinkedHashMap<>();
                col.put("key", week + "(" + version + ")");
                col.put("week", week);
                col.put("weekLabel", weekLabel);
                col.put("version", version);
                columns.add(col);
            }
        }

        // 按 itemNumber 分组，然后按(周, 版本)汇总
        Map<String, Map<String, BigDecimal>> itemData = new LinkedHashMap<>();
        Map<String, String> itemDescriptions = new LinkedHashMap<>();

        for (MrpPlan plan : plans) {
            String itemKey = plan.getItemNumber();
            LocalDate date = plan.getReleaseDate();
            int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
            int year = date.get(weekFields.weekBasedYear());
            String weekKey = String.format("%dW%02d", year, weekOfYear);
            String columnKey = weekKey + "(" + plan.getVersion() + ")";

            itemData.putIfAbsent(itemKey, new LinkedHashMap<>());
            Map<String, BigDecimal> row = itemData.get(itemKey);
            BigDecimal currentQty = row.getOrDefault(columnKey, BigDecimal.ZERO);
            row.put(columnKey, currentQty.add(plan.getQuantityScheduled()));

            if (plan.getDescription() != null) {
                itemDescriptions.put(itemKey, plan.getDescription());
            }
        }

        // 构建结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> itemEntry : itemData.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            String itemNumber = itemEntry.getKey();
            row.put("itemNumber", itemNumber);
            row.put("description", itemDescriptions.getOrDefault(itemNumber, ""));

            Map<String, BigDecimal> data = itemEntry.getValue();
            for (Map<String, String> col : columns) {
                String key = col.get("key");
                BigDecimal qty = data.get(key);
                row.put(key, qty != null ? qty : null);
            }

            result.add(row);
        }

        // 返回包含columns信息
        Map<String, Object> resultWrapper = new LinkedHashMap<>();
        resultWrapper.put("columns", columns);
        resultWrapper.put("data", result);
        resultWrapper.put("weekDateRanges", weekDateRanges);

        List<Map<String, Object>> returnResult = new ArrayList<>();
        returnResult.add(resultWrapper);

        return returnResult;
    }

    // 单版本周需求汇总 - 按导入人+文件名+版本
    public Map<String, Object> getWeeklyDemandByVersion(String createdBy, String fileName, String version) {
        List<MrpPlan> plans = repository.findByCreatedByAndFileNameAndVersionOrderByItemNumberAndReleaseDate(createdBy, fileName, version);

        // 使用ISO标准周（周一到周日）
        java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;

        // 获取所有周及其标准日期范围（周一到周日），使用年份+周作为key保证排序正确
        Map<String, LocalDate[]> weekDateRanges = new TreeMap<>(Comparator.naturalOrder());

        for (MrpPlan plan : plans) {
            LocalDate date = plan.getReleaseDate();
            int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
            int year = date.get(weekFields.weekBasedYear());
            String weekKey = String.format("%dW%02d", year, weekOfYear);

            if (!weekDateRanges.containsKey(weekKey)) {
                LocalDate monday = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                LocalDate sunday = monday.plusDays(6);
                weekDateRanges.put(weekKey, new LocalDate[]{monday, sunday});
            }
        }

        // 按 itemNumber 分组，然后按周汇总
        Map<String, Map<String, BigDecimal>> itemData = new LinkedHashMap<>();
        Map<String, String> itemDescriptions = new LinkedHashMap<>();
        Map<String, BigDecimal> itemTotals = new LinkedHashMap<>();

        for (MrpPlan plan : plans) {
            String itemKey = plan.getItemNumber();
            LocalDate date = plan.getReleaseDate();
            int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
            int year = date.get(weekFields.weekBasedYear());
            String weekKey = String.format("%dW%02d", year, weekOfYear);

            itemData.putIfAbsent(itemKey, new LinkedHashMap<>());
            Map<String, BigDecimal> row = itemData.get(itemKey);
            BigDecimal currentQty = row.getOrDefault(weekKey, BigDecimal.ZERO);
            row.put(weekKey, currentQty.add(plan.getQuantityScheduled()));

            if (plan.getDescription() != null) {
                itemDescriptions.put(itemKey, plan.getDescription());
            }

            // 计算每个item的total
            BigDecimal total = itemTotals.getOrDefault(itemKey, BigDecimal.ZERO);
            itemTotals.put(itemKey, total.add(plan.getQuantityScheduled()));
        }

        // 构建结果
        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> itemEntry : itemData.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            String itemNumber = itemEntry.getKey();
            row.put("itemNumber", itemNumber);
            row.put("description", itemDescriptions.getOrDefault(itemNumber, ""));
            row.put("weeks", itemEntry.getValue());
            row.put("total", itemTotals.getOrDefault(itemNumber, BigDecimal.ZERO));
            data.add(row);
        }

        // 直接返回weekDateRanges（LocalDate数组）
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("weekDateRanges", weekDateRanges);

        return result;
    }

    // 单版本月需求汇总 - 按导入人+文件名+版本
    public Map<String, Object> getMonthlyDemandByVersion(String createdBy, String fileName, String version) {
        List<MrpPlan> plans = repository.findByCreatedByAndFileNameAndVersionOrderByItemNumberAndReleaseDate(createdBy, fileName, version);

        // 获取所有月份及其日期范围，按年月排序
        Map<String, LocalDate[]> monthDateRanges = new TreeMap<>(Comparator.naturalOrder());

        for (MrpPlan plan : plans) {
            LocalDate date = plan.getReleaseDate();
            int year = date.getYear();
            int month = date.getMonthValue();
            String monthKey = String.format("%d-%02d", year, month);

            if (!monthDateRanges.containsKey(monthKey)) {
                LocalDate firstDay = date.withDayOfMonth(1);
                LocalDate lastDay = date.with(TemporalAdjusters.lastDayOfMonth());
                monthDateRanges.put(monthKey, new LocalDate[]{firstDay, lastDay});
            }
        }

        // 按 itemNumber 分组，然后按月汇总
        Map<String, Map<String, BigDecimal>> itemData = new LinkedHashMap<>();
        Map<String, String> itemDescriptions = new LinkedHashMap<>();
        Map<String, BigDecimal> itemTotals = new LinkedHashMap<>();

        for (MrpPlan plan : plans) {
            String itemKey = plan.getItemNumber();
            LocalDate date = plan.getReleaseDate();
            int year = date.getYear();
            int month = date.getMonthValue();
            String monthKey = String.format("%d-%02d", year, month);

            itemData.putIfAbsent(itemKey, new LinkedHashMap<>());
            Map<String, BigDecimal> row = itemData.get(itemKey);
            BigDecimal currentQty = row.getOrDefault(monthKey, BigDecimal.ZERO);
            row.put(monthKey, currentQty.add(plan.getQuantityScheduled()));

            if (plan.getDescription() != null) {
                itemDescriptions.put(itemKey, plan.getDescription());
            }

            // 计算每个item的total
            BigDecimal total = itemTotals.getOrDefault(itemKey, BigDecimal.ZERO);
            itemTotals.put(itemKey, total.add(plan.getQuantityScheduled()));
        }

        // 构建结果
        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> itemEntry : itemData.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            String itemNumber = itemEntry.getKey();
            row.put("itemNumber", itemNumber);
            row.put("description", itemDescriptions.getOrDefault(itemNumber, ""));
            row.put("months", itemEntry.getValue());
            row.put("total", itemTotals.getOrDefault(itemNumber, BigDecimal.ZERO));
            data.add(row);
        }

        // 转换monthDateRanges的日期为字符串格式
        Map<String, String[]> monthDateRangesStr = new TreeMap<>(Comparator.naturalOrder());
        for (Map.Entry<String, LocalDate[]> entry : monthDateRanges.entrySet()) {
            LocalDate[] range = entry.getValue();
            monthDateRangesStr.put(entry.getKey(), new String[]{
                range[0].toString(),
                range[1].toString()
            });
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("monthDateRanges", monthDateRangesStr);

        return result;
    }

    // 月报表 - 按导入人+文件名，获取所有版本对比数据
    // 格式：Item Number | Description | 1月-2026 | 2月-2026 | 3月-2026 | ...
    public List<Map<String, Object>> getMonthlyReportByCreatedByAndFileName(String createdBy, String fileName) {
        List<MrpPlan> plans = repository.findByCreatedByAndFileNameOrderByItemNumberAndReleaseDate(createdBy, fileName);

        // 获取所有版本
        List<String> allVersions = plans.stream()
                .map(MrpPlan::getVersion)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // 获取所有月份及其日期范围，按年月排序
        Map<String, LocalDate[]> monthDateRanges = new TreeMap<>(Comparator.naturalOrder());

        for (MrpPlan plan : plans) {
            LocalDate date = plan.getReleaseDate();
            int year = date.getYear();
            int month = date.getMonthValue();
            // 使用年月作为key，如 "2026-01"
            String monthKey = String.format("%d-%02d", year, month);

            if (!monthDateRanges.containsKey(monthKey)) {
                // 计算该月的第一天和最后一天
                LocalDate firstDay = date.withDayOfMonth(1);
                LocalDate lastDay = date.with(TemporalAdjusters.lastDayOfMonth());
                monthDateRanges.put(monthKey, new LocalDate[]{firstDay, lastDay});
            }
        }

        // 生成列名：按年月顺序排列
        List<Map<String, String>> columns = new ArrayList<>();
        for (String month : monthDateRanges.keySet()) {
            LocalDate[] range = monthDateRanges.get(month);
            // 解析年月用于显示
            String[] parts = month.split("-");
            int year = Integer.parseInt(parts[0]);
            int monthNum = Integer.parseInt(parts[1]);
            String monthLabel = monthNum + "月" + year + "(" + range[0].getMonthValue() + "/" + range[0].getDayOfMonth() + "-" + range[1].getMonthValue() + "/" + range[1].getDayOfMonth() + ")";
            for (String version : allVersions) {
                Map<String, String> col = new LinkedHashMap<>();
                col.put("key", month + "(" + version + ")");
                col.put("month", month);
                col.put("monthLabel", monthLabel);
                col.put("version", version);
                columns.add(col);
            }
        }

        // 按 itemNumber 分组，然后按(月, 版本)汇总
        Map<String, Map<String, BigDecimal>> itemData = new LinkedHashMap<>();
        Map<String, String> itemDescriptions = new LinkedHashMap<>();

        for (MrpPlan plan : plans) {
            String itemKey = plan.getItemNumber();
            LocalDate date = plan.getReleaseDate();
            int year = date.getYear();
            int month = date.getMonthValue();
            String monthKey = String.format("%d-%02d", year, month);
            String columnKey = monthKey + "(" + plan.getVersion() + ")";

            itemData.putIfAbsent(itemKey, new LinkedHashMap<>());
            Map<String, BigDecimal> row = itemData.get(itemKey);
            BigDecimal currentQty = row.getOrDefault(columnKey, BigDecimal.ZERO);
            row.put(columnKey, currentQty.add(plan.getQuantityScheduled()));

            if (plan.getDescription() != null) {
                itemDescriptions.put(itemKey, plan.getDescription());
            }
        }

        // 构建结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> itemEntry : itemData.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            String itemNumber = itemEntry.getKey();
            row.put("itemNumber", itemNumber);
            row.put("description", itemDescriptions.getOrDefault(itemNumber, ""));

            Map<String, BigDecimal> data = itemEntry.getValue();
            for (Map<String, String> col : columns) {
                String key = col.get("key");
                BigDecimal qty = data.get(key);
                row.put(key, qty != null ? qty : null);
            }

            result.add(row);
        }

        // 返回包含columns信息
        Map<String, Object> resultWrapper = new LinkedHashMap<>();
        resultWrapper.put("columns", columns);
        resultWrapper.put("data", result);
        resultWrapper.put("monthDateRanges", monthDateRanges);

        List<Map<String, Object>> returnResult = new ArrayList<>();
        returnResult.add(resultWrapper);

        return returnResult;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private MrpPlanDto toDto(MrpPlan entity) {
        MrpPlanDto dto = new MrpPlanDto();
        dto.setId(entity.getId());
        dto.setItemNumber(entity.getItemNumber());
        dto.setDescription(entity.getDescription());
        dto.setSite(entity.getSite());
        dto.setProductionLine(entity.getProductionLine());
        dto.setReleaseDate(entity.getReleaseDate());
        dto.setDueDate(entity.getDueDate());
        dto.setQuantityScheduled(entity.getQuantityScheduled());
        dto.setQuantityCompleted(entity.getQuantityCompleted());
        dto.setRoutingCode(entity.getRoutingCode());
        dto.setVersion(entity.getVersion());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setFileName(entity.getFileName());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toLocalDate() : null);
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toLocalDate() : null);
        return dto;
    }

    private MrpPlan toEntity(MrpPlanDto dto) {
        MrpPlan entity = new MrpPlan();
        if (dto.getId() != null) {
            entity.setId(dto.getId());
        }
        entity.setItemNumber(dto.getItemNumber());
        entity.setDescription(dto.getDescription());
        entity.setSite(dto.getSite());
        entity.setProductionLine(dto.getProductionLine());
        entity.setReleaseDate(dto.getReleaseDate());
        entity.setDueDate(dto.getDueDate());
        entity.setQuantityScheduled(dto.getQuantityScheduled());
        entity.setQuantityCompleted(dto.getQuantityCompleted());
        entity.setRoutingCode(dto.getRoutingCode());
        entity.setVersion(dto.getVersion());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setFileName(dto.getFileName());
        return entity;
    }
}

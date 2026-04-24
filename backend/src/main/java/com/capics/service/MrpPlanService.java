package com.capics.service;

import com.capics.dto.MrpPlanDto;
import com.capics.entity.MrpPlan;
import com.capics.entity.Product;
import com.capics.repository.MrpPlanRepository;
import com.capics.repository.ProductRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class MrpPlanService {

    private final MrpPlanRepository repository;
    private final ProductRepository productRepository;

    public MrpPlanService(MrpPlanRepository repository, ProductRepository productRepository) {
        this.repository = repository;
        this.productRepository = productRepository;
    }

    public List<MrpPlanDto> findAll() {
        return toDtoListWithProductDescription(repository.findAll());
    }

    public List<MrpPlanDto> findByVersion(String version) {
        return toDtoListWithProductDescription(repository.findByVersion(version));
    }

    public List<String> getAllVersions() {
        return repository.findAllVersions();
    }

    public MrpPlanDto findById(Long id) {
        return repository.findById(id)
                .map(entity -> toDto(entity, buildProductDescriptionMap(Collections.singleton(entity.getItemNumber()))))
                .orElseThrow(() -> new RuntimeException("MRP Plan not found"));
    }

    public MrpPlanDto save(MrpPlanDto dto) {
        MrpPlan entity = toEntity(dto);
        entity = repository.save(entity);
        return toDto(entity, buildProductDescriptionMap(Collections.singleton(entity.getItemNumber())));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

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
        if (repository.existsByCreatedByAndFileName(createdBy, fileName)) {
            throw new IOException("File '" + fileName + "' already exists for this user.");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("Data");
            if (sheet == null) {
                throw new IOException("Invalid MRP template: missing sheet 'Data'.");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("Invalid MRP template: missing header row in 'Data' sheet.");
            }

            Map<String, Integer> headerMap = new HashMap<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                String header = getCellValueAsString(headerRow.getCell(c));
                if (header != null) {
                    headerMap.put(header.trim().toLowerCase(Locale.ROOT), c);
                }
            }

            Integer idxItemNumber = findColumnIndex(headerMap, "item number*", "item number");
            Integer idxReleaseDate = findColumnIndex(headerMap, "release date*", "release date");
            Integer idxDueDate = findColumnIndex(headerMap, "due date*", "due date");
            Integer idxQuantity = findColumnIndex(headerMap, "quantity*", "quantity");
            Integer idxReference = findColumnIndex(headerMap, "reference*", "reference");

            if (idxItemNumber == null || idxReleaseDate == null || idxDueDate == null || idxQuantity == null || idxReference == null) {
                throw new IOException("Invalid MRP template: required headers are Item Number*, Release Date*, Due Date*, Quantity*, Reference*.");
            }

            List<MrpPlan> importCandidates = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                MrpPlan entity = new MrpPlan();
                entity.setItemNumber(trimToNull(getCellValueAsString(row.getCell(idxItemNumber))));
                entity.setReleaseDate(getCellValueAsDate(row.getCell(idxReleaseDate)));
                entity.setDueDate(getCellValueAsDate(row.getCell(idxDueDate)));
                entity.setQuantityScheduled(getCellValueAsBigDecimal(row.getCell(idxQuantity)));
                entity.setVersion(trimToNull(getCellValueAsString(row.getCell(idxReference))));

                entity.setDescription(null);
                entity.setSite("1240");
                entity.setProductionLine(null);
                entity.setRoutingCode(null);
                entity.setQuantityCompleted(BigDecimal.ZERO);
                entity.setFileName(fileName);
                entity.setCreatedBy(createdBy);

                if (entity.getItemNumber() != null && entity.getVersion() != null) {
                    if (entity.getReleaseDate() == null) {
                        throw new IOException("Row " + (i + 1) + " missing required field: Release Date*");
                    }
                    if (entity.getDueDate() == null) {
                        throw new IOException("Row " + (i + 1) + " missing required field: Due Date*");
                    }
                    if (entity.getQuantityScheduled() == null) {
                        throw new IOException("Row " + (i + 1) + " missing required field: Quantity*");
                    }
                    importCandidates.add(entity);
                }
            }

            if (importCandidates.isEmpty()) {
                return 0;
            }

            YearMonth startMonth = importCandidates.stream()
                    .map(MrpPlan::getReleaseDate)
                    .filter(Objects::nonNull)
                    .map(YearMonth::from)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            if (startMonth == null) {
                return 0;
            }
            YearMonth endExclusive = startMonth.plusMonths(12);

            List<MrpPlan> filteredPlans = importCandidates.stream()
                    .filter(plan -> {
                        LocalDate releaseDate = plan.getReleaseDate();
                        if (releaseDate == null) {
                            return false;
                        }
                        YearMonth ym = YearMonth.from(releaseDate);
                        return !ym.isBefore(startMonth) && ym.isBefore(endExclusive);
                    })
                    .collect(Collectors.toList());

            repository.saveAll(filteredPlans);
            return filteredPlans.size();
        }
    }

    public List<Map<String, Object>> getWeeklyReport(String version) {
        List<MrpPlan> plans = repository.findByVersionOrderByItemNumberAndReleaseDate(version);
        Map<String, String> itemDescriptions = buildDescriptionMapFromPlans(plans);

        Map<String, Map<String, BigDecimal>> report = new LinkedHashMap<>();
        for (MrpPlan plan : plans) {
            if (plan.getReleaseDate() == null || plan.getItemNumber() == null) {
                continue;
            }
            String itemKey = plan.getItemNumber();
            report.putIfAbsent(itemKey, new LinkedHashMap<>());

            int weekOfYear = plan.getReleaseDate().get(java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfYear());
            String weekKey = String.format("%02d", weekOfYear);

            Map<String, BigDecimal> itemData = report.get(itemKey);
            BigDecimal currentQty = itemData.getOrDefault(weekKey, BigDecimal.ZERO);
            itemData.put(weekKey, currentQty.add(safeQty(plan.getQuantityScheduled())));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : report.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemNumber", entry.getKey());
            row.put("description", itemDescriptions.getOrDefault(normalizeKey(entry.getKey()), ""));
            row.put("weeklyDemand", entry.getValue());
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> getMonthlyReport(String version) {
        List<MrpPlan> plans = repository.findByVersionOrderByItemNumberAndReleaseDate(version);
        Map<String, String> itemDescriptions = buildDescriptionMapFromPlans(plans);

        Map<String, Map<String, BigDecimal>> report = new LinkedHashMap<>();
        for (MrpPlan plan : plans) {
            if (plan.getReleaseDate() == null || plan.getItemNumber() == null) {
                continue;
            }
            String itemKey = plan.getItemNumber();
            report.putIfAbsent(itemKey, new LinkedHashMap<>());

            String monthKey = String.format("%d-%02d", plan.getReleaseDate().getYear(), plan.getReleaseDate().getMonthValue());
            Map<String, BigDecimal> itemData = report.get(itemKey);
            BigDecimal currentQty = itemData.getOrDefault(monthKey, BigDecimal.ZERO);
            itemData.put(monthKey, currentQty.add(safeQty(plan.getQuantityScheduled())));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : report.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemNumber", entry.getKey());
            row.put("description", itemDescriptions.getOrDefault(normalizeKey(entry.getKey()), ""));
            row.put("monthlyDemand", entry.getValue());
            result.add(row);
        }
        return result;
    }

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
        return toDtoListWithProductDescription(repository.findByCreatedByAndFileNameAndVersion(createdBy, fileName, version));
    }

    public List<MrpPlanDto> findByCreatedByAndFileName(String createdBy, String fileName) {
        return toDtoListWithProductDescription(repository.findByCreatedByAndFileName(createdBy, fileName));
    }

    public List<Map<String, Object>> getWeeklyReportByCreatedByAndFileName(String createdBy, String fileName) {
        List<MrpPlan> plans = repository.findByCreatedByAndFileNameOrderByItemNumberAndReleaseDate(createdBy, fileName);
        Map<String, String> itemDescriptions = buildDescriptionMapFromPlans(plans);

        List<String> allVersions = plans.stream()
                .map(MrpPlan::getVersion)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;
        Map<String, LocalDate[]> weekDateRanges = new TreeMap<>(Comparator.naturalOrder());

        for (MrpPlan plan : plans) {
            LocalDate date = plan.getReleaseDate();
            if (date == null) {
                continue;
            }
            int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
            int year = date.get(weekFields.weekBasedYear());
            String weekKey = String.format("%dW%02d", year, weekOfYear);

            weekDateRanges.computeIfAbsent(weekKey, key -> {
                LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate sunday = monday.plusDays(6);
                return new LocalDate[]{monday, sunday};
            });
        }

        List<Map<String, String>> columns = new ArrayList<>();
        for (String week : weekDateRanges.keySet()) {
            LocalDate[] range = weekDateRanges.get(week);
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

        Map<String, Map<String, BigDecimal>> itemData = new LinkedHashMap<>();
        for (MrpPlan plan : plans) {
            String itemKey = trimToNull(plan.getItemNumber());
            LocalDate date = plan.getReleaseDate();
            String version = trimToNull(plan.getVersion());
            if (itemKey == null || date == null || version == null) {
                continue;
            }
            int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
            int year = date.get(weekFields.weekBasedYear());
            String weekKey = String.format("%dW%02d", year, weekOfYear);
            String columnKey = weekKey + "(" + version + ")";

            itemData.putIfAbsent(itemKey, new LinkedHashMap<>());
            Map<String, BigDecimal> row = itemData.get(itemKey);
            BigDecimal currentQty = row.getOrDefault(columnKey, BigDecimal.ZERO);
            row.put(columnKey, currentQty.add(safeQty(plan.getQuantityScheduled())));
        }

        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> itemEntry : itemData.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            String itemNumber = itemEntry.getKey();
            row.put("itemNumber", itemNumber);
            row.put("description", itemDescriptions.getOrDefault(normalizeKey(itemNumber), ""));

            Map<String, BigDecimal> values = itemEntry.getValue();
            for (Map<String, String> col : columns) {
                String key = col.get("key");
                BigDecimal qty = values.get(key);
                row.put(key, qty != null ? qty : null);
            }
            data.add(row);
        }

        Map<String, Object> resultWrapper = new LinkedHashMap<>();
        resultWrapper.put("columns", columns);
        resultWrapper.put("data", data);
        resultWrapper.put("weekDateRanges", weekDateRanges);

        List<Map<String, Object>> returnResult = new ArrayList<>();
        returnResult.add(resultWrapper);
        return returnResult;
    }

    public Map<String, Object> getWeeklyDemandByVersion(String createdBy, String fileName, String version) {
        List<MrpPlan> plans = repository.findByCreatedByAndFileNameAndVersionOrderByItemNumberAndReleaseDate(createdBy, fileName, version);
        Map<String, String> itemDescriptions = buildDescriptionMapFromPlans(plans);

        java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;
        Map<String, LocalDate[]> weekDateRanges = new TreeMap<>(Comparator.naturalOrder());

        for (MrpPlan plan : plans) {
            LocalDate date = plan.getReleaseDate();
            if (date == null) {
                continue;
            }
            int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
            int year = date.get(weekFields.weekBasedYear());
            String weekKey = String.format("%dW%02d", year, weekOfYear);
            weekDateRanges.computeIfAbsent(weekKey, key -> {
                LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate sunday = monday.plusDays(6);
                return new LocalDate[]{monday, sunday};
            });
        }

        Map<String, Map<String, BigDecimal>> itemData = new LinkedHashMap<>();
        Map<String, BigDecimal> itemTotals = new LinkedHashMap<>();

        for (MrpPlan plan : plans) {
            String itemKey = trimToNull(plan.getItemNumber());
            LocalDate date = plan.getReleaseDate();
            if (itemKey == null || date == null) {
                continue;
            }
            int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
            int year = date.get(weekFields.weekBasedYear());
            String weekKey = String.format("%dW%02d", year, weekOfYear);

            itemData.putIfAbsent(itemKey, new LinkedHashMap<>());
            Map<String, BigDecimal> row = itemData.get(itemKey);
            BigDecimal qty = safeQty(plan.getQuantityScheduled());
            row.put(weekKey, row.getOrDefault(weekKey, BigDecimal.ZERO).add(qty));
            itemTotals.put(itemKey, itemTotals.getOrDefault(itemKey, BigDecimal.ZERO).add(qty));
        }

        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> itemEntry : itemData.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            String itemNumber = itemEntry.getKey();
            row.put("itemNumber", itemNumber);
            row.put("description", itemDescriptions.getOrDefault(normalizeKey(itemNumber), ""));
            row.put("weeks", itemEntry.getValue());
            row.put("total", itemTotals.getOrDefault(itemNumber, BigDecimal.ZERO));
            data.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("weekDateRanges", weekDateRanges);
        return result;
    }

    public Map<String, Object> getMonthlyDemandByVersion(String createdBy, String fileName, String version) {
        List<MrpPlan> plans = repository.findByCreatedByAndFileNameAndVersionOrderByItemNumberAndReleaseDate(createdBy, fileName, version);
        Map<String, String> itemDescriptions = buildDescriptionMapFromPlans(plans);

        Map<String, LocalDate[]> monthDateRanges = new TreeMap<>(Comparator.naturalOrder());
        for (MrpPlan plan : plans) {
            LocalDate date = plan.getReleaseDate();
            if (date == null) {
                continue;
            }
            String monthKey = String.format("%d-%02d", date.getYear(), date.getMonthValue());
            monthDateRanges.computeIfAbsent(monthKey, key -> {
                LocalDate firstDay = date.withDayOfMonth(1);
                LocalDate lastDay = date.with(TemporalAdjusters.lastDayOfMonth());
                return new LocalDate[]{firstDay, lastDay};
            });
        }

        Map<String, Map<String, BigDecimal>> itemData = new LinkedHashMap<>();
        Map<String, BigDecimal> itemTotals = new LinkedHashMap<>();

        for (MrpPlan plan : plans) {
            String itemKey = trimToNull(plan.getItemNumber());
            LocalDate date = plan.getReleaseDate();
            if (itemKey == null || date == null) {
                continue;
            }
            String monthKey = String.format("%d-%02d", date.getYear(), date.getMonthValue());

            itemData.putIfAbsent(itemKey, new LinkedHashMap<>());
            Map<String, BigDecimal> row = itemData.get(itemKey);
            BigDecimal qty = safeQty(plan.getQuantityScheduled());
            row.put(monthKey, row.getOrDefault(monthKey, BigDecimal.ZERO).add(qty));
            itemTotals.put(itemKey, itemTotals.getOrDefault(itemKey, BigDecimal.ZERO).add(qty));
        }

        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> itemEntry : itemData.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            String itemNumber = itemEntry.getKey();
            row.put("itemNumber", itemNumber);
            row.put("description", itemDescriptions.getOrDefault(normalizeKey(itemNumber), ""));
            row.put("months", itemEntry.getValue());
            row.put("total", itemTotals.getOrDefault(itemNumber, BigDecimal.ZERO));
            data.add(row);
        }

        Map<String, String[]> monthDateRangesStr = new TreeMap<>(Comparator.naturalOrder());
        for (Map.Entry<String, LocalDate[]> entry : monthDateRanges.entrySet()) {
            LocalDate[] range = entry.getValue();
            monthDateRangesStr.put(entry.getKey(), new String[]{range[0].toString(), range[1].toString()});
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("monthDateRanges", monthDateRangesStr);
        return result;
    }

    public List<Map<String, Object>> getMonthlyReportByCreatedByAndFileName(String createdBy, String fileName) {
        List<MrpPlan> plans = repository.findByCreatedByAndFileNameOrderByItemNumberAndReleaseDate(createdBy, fileName);
        Map<String, String> itemDescriptions = buildDescriptionMapFromPlans(plans);

        List<String> allVersions = plans.stream()
                .map(MrpPlan::getVersion)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Map<String, LocalDate[]> monthDateRanges = new TreeMap<>(Comparator.naturalOrder());
        for (MrpPlan plan : plans) {
            LocalDate date = plan.getReleaseDate();
            if (date == null) {
                continue;
            }
            String monthKey = String.format("%d-%02d", date.getYear(), date.getMonthValue());
            monthDateRanges.computeIfAbsent(monthKey, key -> {
                LocalDate firstDay = date.withDayOfMonth(1);
                LocalDate lastDay = date.with(TemporalAdjusters.lastDayOfMonth());
                return new LocalDate[]{firstDay, lastDay};
            });
        }

        List<Map<String, String>> columns = new ArrayList<>();
        for (String month : monthDateRanges.keySet()) {
            LocalDate[] range = monthDateRanges.get(month);
            String[] parts = month.split("-");
            int year = Integer.parseInt(parts[0]);
            int monthNum = Integer.parseInt(parts[1]);
            String monthLabel = monthNum + "月" + year + "(" + range[0].getMonthValue() + "/" + range[0].getDayOfMonth() + "-" + range[1].getMonthValue() + "/" + range[1].getDayOfMonth() + ")";
            for (String ver : allVersions) {
                Map<String, String> col = new LinkedHashMap<>();
                col.put("key", month + "(" + ver + ")");
                col.put("month", month);
                col.put("monthLabel", monthLabel);
                col.put("version", ver);
                columns.add(col);
            }
        }

        Map<String, Map<String, BigDecimal>> itemData = new LinkedHashMap<>();
        for (MrpPlan plan : plans) {
            String itemKey = trimToNull(plan.getItemNumber());
            String ver = trimToNull(plan.getVersion());
            LocalDate date = plan.getReleaseDate();
            if (itemKey == null || ver == null || date == null) {
                continue;
            }
            String monthKey = String.format("%d-%02d", date.getYear(), date.getMonthValue());
            String columnKey = monthKey + "(" + ver + ")";

            itemData.putIfAbsent(itemKey, new LinkedHashMap<>());
            Map<String, BigDecimal> row = itemData.get(itemKey);
            row.put(columnKey, row.getOrDefault(columnKey, BigDecimal.ZERO).add(safeQty(plan.getQuantityScheduled())));
        }

        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> itemEntry : itemData.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            String itemNumber = itemEntry.getKey();
            row.put("itemNumber", itemNumber);
            row.put("description", itemDescriptions.getOrDefault(normalizeKey(itemNumber), ""));

            Map<String, BigDecimal> values = itemEntry.getValue();
            for (Map<String, String> col : columns) {
                String key = col.get("key");
                BigDecimal qty = values.get(key);
                row.put(key, qty != null ? qty : null);
            }
            data.add(row);
        }

        Map<String, Object> resultWrapper = new LinkedHashMap<>();
        resultWrapper.put("columns", columns);
        resultWrapper.put("data", data);
        resultWrapper.put("monthDateRanges", monthDateRanges);

        List<Map<String, Object>> returnResult = new ArrayList<>();
        returnResult.add(resultWrapper);
        return returnResult;
    }

    private Integer findColumnIndex(Map<String, Integer> headerMap, String... aliases) {
        for (String alias : aliases) {
            Integer idx = headerMap.get(alias.toLowerCase(Locale.ROOT));
            if (idx != null) {
                return idx;
            }
        }
        return null;
    }

    private LocalDate getCellValueAsDate(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            Date date = DateUtil.getJavaDate(cell.getNumericCellValue());
            return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }

        String value = getCellValueAsString(cell);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim();
        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/M/d"),
                DateTimeFormatter.ofPattern("yyyy/M/dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/d"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }

        String value = getCellValueAsString(cell);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(value.trim().replace(",", ""));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
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

    private List<MrpPlanDto> toDtoListWithProductDescription(List<MrpPlan> entities) {
        Map<String, String> descriptionMap = buildDescriptionMapFromPlans(entities);
        return entities.stream()
                .map(entity -> toDto(entity, descriptionMap))
                .collect(Collectors.toList());
    }

    private MrpPlanDto toDto(MrpPlan entity, Map<String, String> productDescriptionMap) {
        MrpPlanDto dto = new MrpPlanDto();
        dto.setId(entity.getId());
        dto.setItemNumber(entity.getItemNumber());

        String mappedDescription = productDescriptionMap.get(normalizeKey(entity.getItemNumber()));
        dto.setDescription(mappedDescription != null ? mappedDescription : entity.getDescription());

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

    private Map<String, String> buildDescriptionMapFromPlans(List<MrpPlan> plans) {
        Set<String> itemNumbers = plans == null ? Collections.emptySet() : plans.stream()
                .map(MrpPlan::getItemNumber)
                .map(this::normalizeKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, String> productDescriptionMap = buildProductDescriptionMap(itemNumbers);

        if (plans != null) {
            for (MrpPlan plan : plans) {
                String key = normalizeKey(plan.getItemNumber());
                if (key == null || productDescriptionMap.containsKey(key)) {
                    continue;
                }
                String fallback = trimToNull(plan.getDescription());
                if (fallback != null) {
                    productDescriptionMap.put(key, fallback);
                }
            }
        }
        return productDescriptionMap;
    }

    private Map<String, String> buildProductDescriptionMap(Collection<String> itemNumbers) {
        if (itemNumbers == null || itemNumbers.isEmpty()) {
            return new LinkedHashMap<>();
        }

        List<String> normalizedItemNumbers = itemNumbers.stream()
                .map(this::normalizeKey)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (normalizedItemNumbers.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Set<String> targetKeys = new LinkedHashSet<>(normalizedItemNumbers);
        Map<String, String> result = new LinkedHashMap<>();

        // 使用全量产品数据做标准化匹配，避免 itemNumber 因空格/大小写/.0 等格式差异导致匹配失败。
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            if (product == null) {
                continue;
            }
            String key = normalizeKey(product.getItemNumber());
            String description = trimToNull(product.getDescription());
            if (key == null || description == null || !targetKeys.contains(key)) {
                continue;
            }
            result.putIfAbsent(key, description);
        }
        return result;
    }

    private String normalizeKey(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String normalized = trimmed.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (normalized.endsWith(".0")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal safeQty(BigDecimal qty) {
        return qty == null ? BigDecimal.ZERO : qty;
    }
}

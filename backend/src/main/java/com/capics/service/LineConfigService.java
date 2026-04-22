package com.capics.service;

import com.capics.dto.LineConfigDto;
import com.capics.entity.LineConfig;
import com.capics.repository.LineConfigRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LineConfigService {

    private final LineConfigRepository repository;

    public LineConfigService(LineConfigRepository repository) {
        this.repository = repository;
    }

    public List<LineConfigDto> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "lineCode")).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<LineConfigDto> findActive() {
        return repository.findByIsActiveTrue().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public LineConfigDto findById(String lineCode) {
        return repository.findById(lineCode)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Line config not found"));
    }

    public LineConfigDto save(LineConfigDto dto) {
        LineConfig entity;
        if (repository.existsById(dto.getLineCode())) {
            entity = repository.findById(dto.getLineCode()).get();
            entity.setLineName(dto.getLineName());
            entity.setWorkingDaysPerWeek(dto.getWorkingDaysPerWeek());
            entity.setShiftsPerDay(dto.getShiftsPerDay());
            entity.setHoursPerShift(dto.getHoursPerShift());
            entity.setIsActive(dto.getIsActive());
            entity.setUpdatedBy(dto.getUpdatedBy());
        } else {
            entity = toEntity(dto);
            entity.setCreatedBy(dto.getUpdatedBy());
            entity.setUpdatedBy(dto.getUpdatedBy());
        }
        entity = repository.save(entity);
        return toDto(entity);
    }

    public boolean exists(String lineCode) {
        return repository.existsById(lineCode);
    }

    public void delete(String lineCode) {
        repository.deleteById(lineCode);
    }

    public int importFromExcel(MultipartFile file, String updatedBy) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IOException("Line template format error: sheet not found");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("Line template format error: header row not found");
            }

            Map<String, Integer> headerMap = new HashMap<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                String header = getCellValueAsString(headerRow.getCell(c));
                if (header != null) {
                    headerMap.put(header.trim().toLowerCase(Locale.ROOT), c);
                }
            }

            Integer idxLineCode = findColumnIndex(
                    headerMap,
                    "line code*", "line code", "line_code", "production line code",
                    "\u751f\u4ea7\u7ebf\u7f16\u7801*", "\u751f\u4ea7\u7ebf\u7f16\u7801"
            );
            Integer idxLineName = findColumnIndex(
                    headerMap,
                    "line name", "line_name", "production line name",
                    "\u751f\u4ea7\u7ebf\u540d\u79f0"
            );
            Integer idxWorkingDays = findColumnIndex(
                    headerMap,
                    "working days per week", "working_days_per_week",
                    "\u6bcf\u5468\u5de5\u4f5c\u5929\u6570"
            );
            Integer idxShifts = findColumnIndex(
                    headerMap,
                    "shifts per day", "shifts_per_day",
                    "\u6bcf\u5929\u73ed\u6b21"
            );
            Integer idxHours = findColumnIndex(
                    headerMap,
                    "hours per shift", "hours_per_shift",
                    "\u6bcf\u73ed\u65f6\u957f(\u5c0f\u65f6)", "\u6bcf\u73ed\u65f6\u957f"
            );
            Integer idxActive = findColumnIndex(
                    headerMap,
                    "is active", "is_active", "status",
                    "\u542f\u7528\u72b6\u6001"
            );

            if (idxLineCode == null) {
                throw new IOException("Line template format error: missing required column Line Code*");
            }

            int count = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String lineCode = getCellValueAsString(row.getCell(idxLineCode));
                if (lineCode == null || lineCode.trim().isEmpty()) {
                    continue;
                }
                lineCode = lineCode.trim();

                LineConfig entity = repository.findById(lineCode).orElseGet(LineConfig::new);
                entity.setLineCode(lineCode);

                if (idxLineName != null) {
                    String lineName = getCellValueAsString(row.getCell(idxLineName));
                    entity.setLineName((lineName == null || lineName.trim().isEmpty()) ? null : lineName.trim());
                }
                if (idxWorkingDays != null) {
                    Integer value = getCellValueAsInteger(row.getCell(idxWorkingDays));
                    if (value != null) {
                        entity.setWorkingDaysPerWeek(value);
                    }
                }
                if (idxShifts != null) {
                    Integer value = getCellValueAsInteger(row.getCell(idxShifts));
                    if (value != null) {
                        entity.setShiftsPerDay(value);
                    }
                }
                if (idxHours != null) {
                    BigDecimal value = getCellValueAsBigDecimal(row.getCell(idxHours));
                    if (value != null) {
                        entity.setHoursPerShift(value);
                    }
                }
                if (idxActive != null) {
                    Boolean value = getCellValueAsBoolean(row.getCell(idxActive));
                    if (value != null) {
                        entity.setIsActive(value);
                    }
                }

                if (entity.getWorkingDaysPerWeek() == null) {
                    entity.setWorkingDaysPerWeek(5);
                }
                if (entity.getShiftsPerDay() == null) {
                    entity.setShiftsPerDay(2);
                }
                if (entity.getHoursPerShift() == null) {
                    entity.setHoursPerShift(new BigDecimal("8.0"));
                }
                if (entity.getIsActive() == null) {
                    entity.setIsActive(true);
                }

                if (entity.getCreatedBy() == null || entity.getCreatedBy().trim().isEmpty()) {
                    entity.setCreatedBy(updatedBy);
                }
                entity.setUpdatedBy(updatedBy);

                repository.save(entity);
                count++;
            }

            return count;
        }
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

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double number = cell.getNumericCellValue();
                if (number == Math.rint(number)) {
                    return String.valueOf((long) number);
                }
                return String.valueOf(number);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception ignored) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ignored2) {
                        return null;
                    }
                }
            default:
                return null;
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        String value = getCellValueAsString(cell);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return (int) Math.round(Double.parseDouble(value.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        String value = getCellValueAsString(cell);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getCellValueAsBoolean(Cell cell) {
        String value = getCellValueAsString(cell);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized)
                || "y".equals(normalized) || "\u542f\u7528".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized)
                || "n".equals(normalized) || "\u7981\u7528".equals(normalized)) {
            return false;
        }
        return null;
    }

    private LineConfigDto toDto(LineConfig entity) {
        LineConfigDto dto = new LineConfigDto();
        dto.setLineCode(entity.getLineCode());
        dto.setLineName(entity.getLineName());
        dto.setWorkingDaysPerWeek(entity.getWorkingDaysPerWeek());
        dto.setShiftsPerDay(entity.getShiftsPerDay());
        dto.setHoursPerShift(entity.getHoursPerShift());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
        return dto;
    }

    private LineConfig toEntity(LineConfigDto dto) {
        LineConfig entity = new LineConfig();
        entity.setLineCode(dto.getLineCode());
        entity.setLineName(dto.getLineName());
        entity.setWorkingDaysPerWeek(dto.getWorkingDaysPerWeek());
        entity.setShiftsPerDay(dto.getShiftsPerDay());
        entity.setHoursPerShift(dto.getHoursPerShift());
        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return entity;
    }
}

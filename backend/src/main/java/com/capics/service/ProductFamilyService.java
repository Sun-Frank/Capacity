package com.capics.service;

import com.capics.dto.ProductFamilyDto;
import com.capics.entity.ProductFamily;
import com.capics.entity.ProductFamilyId;
import com.capics.repository.ProductFamilyRepository;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductFamilyService {

    private static final Logger log = LoggerFactory.getLogger(ProductFamilyService.class);
    private final ProductFamilyRepository repository;

    public ProductFamilyService(ProductFamilyRepository repository) {
        this.repository = repository;
    }

    public List<ProductFamilyDto> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ProductFamilyDto> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return repository.findByFamilyCodeContainingIgnoreCase(keyword.trim()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProductFamilyDto findById(String familyCode, String lineCode) {
        ProductFamilyId id = new ProductFamilyId(familyCode, lineCode);
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Product family not found"));
    }

    public ProductFamilyDto save(ProductFamilyDto dto) {
        log.info("ProductFamilyService.save (create) called with: familyCode={}, lineCode={}",
                dto.getFamilyCode(), dto.getLineCode());
        ProductFamily entity = toEntity(dto);
        entity.setCreatedBy(dto.getUpdatedBy());
        entity = repository.save(entity);
        return toDto(entity);
    }

    @Transactional
    public ProductFamilyDto save(ProductFamilyDto dto, String originalFamilyCode, String originalLineCode) {
        log.info("ProductFamilyService.save called with: familyCode={}, lineCode={}, description={}, pf={}, updatedBy={}",
                dto.getFamilyCode(), dto.getLineCode(), dto.getDescription(), dto.getPf(), dto.getUpdatedBy());

        ProductFamilyId id = new ProductFamilyId(dto.getFamilyCode(), dto.getLineCode());
        Optional<ProductFamily> existing = repository.findById(id);
        log.info("findById result: present={}", existing.isPresent());

        if (existing.isPresent()) {
            ProductFamily entity = existing.get();
            entity.setCodingRule(dto.getCodingRule());
            entity.setCycleTime(dto.getCycleTime());
            entity.setOee(dto.getOee());
            entity.setWorkerCount(dto.getWorkerCount());
            entity.setVersion(dto.getVersion());
            entity.setDescription(dto.getDescription());
            entity.setPf(dto.getPf());
            entity.setUpdatedBy(dto.getUpdatedBy());
            entity = repository.save(entity);
            return toDto(entity);
        } else {
            ProductFamily entity = toEntity(dto);
            entity.setCreatedBy(dto.getUpdatedBy());
            entity = repository.save(entity);
            return toDto(entity);
        }
    }

    public void delete(String familyCode, String lineCode) {
        ProductFamilyId id = new ProductFamilyId(familyCode, lineCode);
        repository.deleteById(id);
    }

    public boolean exists(String familyCode, String lineCode) {
        ProductFamilyId id = new ProductFamilyId(familyCode, lineCode);
        return repository.existsById(id);
    }

    public List<Map<String, String>> checkDuplicates(MultipartFile file) throws IOException {
        List<Map<String, String>> duplicates = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return duplicates;

            Map<String, Integer> headerMap = buildHeaderMap(headerRow);

            Integer idxFamilyCode = findColumnIndex(headerMap,
                    "familycode", "family_code", "family code", "family", "编码族", "familyCode");
            Integer idxLineCode = findColumnIndex(headerMap,
                    "linecode", "line_code", "line code", "line", "生产线", "lineCode");

            if (idxFamilyCode == null || idxLineCode == null) {
                return duplicates;
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String familyCode = getCellValueAsString(row.getCell(idxFamilyCode));
                String lineCode = getCellValueAsString(row.getCell(idxLineCode));

                if (isNotBlank(familyCode) && isNotBlank(lineCode) && exists(familyCode, lineCode)) {
                    Map<String, String> dup = new HashMap<>();
                    dup.put("familyCode", familyCode);
                    dup.put("lineCode", lineCode);
                    duplicates.add(dup);
                }
            }
        }

        return duplicates;
    }

    public int importFromExcel(MultipartFile file, String createdBy) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int count = 0;

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Excel file missing header row");
            }

            Map<String, Integer> headerMap = buildHeaderMap(headerRow);

            Integer idxFamilyCode = findColumnIndex(headerMap,
                    "familycode", "family_code", "family code", "family", "编码族", "familyCode");
            Integer idxLineCode = findColumnIndex(headerMap,
                    "linecode", "line_code", "line code", "line", "生产线", "lineCode");
            Integer idxCodingRule = findColumnIndex(headerMap,
                    "codingrule", "coding_rule", "coding rule", "编码规则", "codingRule");
            Integer idxCycleTime = findColumnIndex(headerMap,
                    "cycletime", "cycle_time", "cycle time", "ct", "周期时间", "cycleTime");
            Integer idxOee = findColumnIndex(headerMap, "oee");
            Integer idxWorkerCount = findColumnIndex(headerMap,
                    "workercount", "worker_count", "worker count", "人数", "workerCount");
            Integer idxVersion = findColumnIndex(headerMap, "version", "版本");
            Integer idxDescription = findColumnIndex(headerMap, "description", "描述", "desc");
            Integer idxPf = findColumnIndex(headerMap, "pf");

            if (idxFamilyCode == null || idxLineCode == null) {
                throw new RuntimeException("Excel missing required columns: familyCode/family code and lineCode/line code");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                ProductFamily entity = new ProductFamily();
                entity.setFamilyCode(getCellValueAsString(row.getCell(idxFamilyCode)));
                entity.setLineCode(getCellValueAsString(row.getCell(idxLineCode)));
                entity.setCodingRule(getCellValueAsString(safeCell(row, idxCodingRule)));
                entity.setCreatedBy(createdBy);

                if (idxCycleTime != null) {
                    BigDecimal ct = getCellNumericAsBigDecimal(row.getCell(idxCycleTime));
                    if (ct != null) entity.setCycleTime(ct);
                }

                if (idxOee != null) {
                    BigDecimal oeeValue = getCellNumericAsBigDecimal(row.getCell(idxOee));
                    if (oeeValue != null) {
                        if (oeeValue.compareTo(BigDecimal.ONE) <= 0) {
                            oeeValue = oeeValue.multiply(BigDecimal.valueOf(100)).setScale(2, java.math.RoundingMode.HALF_UP);
                        }
                        entity.setOee(oeeValue);
                    }
                }

                if (idxWorkerCount != null) {
                    BigDecimal workers = getCellNumericAsBigDecimal(row.getCell(idxWorkerCount));
                    if (workers != null) {
                        entity.setWorkerCount(workers.intValue());
                    }
                }

                if (idxVersion != null) {
                    entity.setVersion(getCellValueAsString(row.getCell(idxVersion)));
                }

                if (idxDescription != null) {
                    entity.setDescription(getCellValueAsString(row.getCell(idxDescription)));
                }

                if (idxPf != null) {
                    entity.setPf(getCellValueAsString(row.getCell(idxPf)));
                }

                if (isNotBlank(entity.getFamilyCode()) && isNotBlank(entity.getLineCode())) {
                    repository.save(entity);
                    count++;
                } else {
                    log.warn("Skip row {} due to blank familyCode/lineCode", i + 1);
                }
            }

            return count;
        }
    }

    private Map<String, Integer> buildHeaderMap(Row headerRow) {
        Map<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            String header = getCellValueAsString(headerRow.getCell(i));
            if (isNotBlank(header)) {
                headerMap.put(normalizeHeader(header), i);
            }
        }
        return headerMap;
    }

    private Integer findColumnIndex(Map<String, Integer> headerMap, String... possibleNames) {
        for (String name : possibleNames) {
            String normalized = normalizeHeader(name);
            if (headerMap.containsKey(normalized)) {
                return headerMap.get(normalized);
            }
        }
        return null;
    }

    private Cell safeCell(Row row, Integer idx) {
        if (idx == null || row == null) {
            return null;
        }
        return row.getCell(idx);
    }

    private BigDecimal getCellNumericAsBigDecimal(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (cell.getCellType() == CellType.STRING) {
                String text = trimToNull(cell.getStringCellValue());
                if (text == null) return null;
                return new BigDecimal(text);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return trimToNull(cell.getStringCellValue());
            case NUMERIC:
                double value = cell.getNumericCellValue();
                if (Math.floor(value) == value) {
                    return String.valueOf((long) value);
                }
                return String.valueOf(value);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private String normalizeHeader(String header) {
        if (header == null) return "";
        return header.trim().toLowerCase().replaceAll("[\\s_\\-]+", "");
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isNotBlank(String value) {
        return trimToNull(value) != null;
    }

    private ProductFamilyDto toDto(ProductFamily entity) {
        ProductFamilyDto dto = new ProductFamilyDto();
        dto.setFamilyCode(entity.getFamilyCode());
        dto.setLineCode(entity.getLineCode());
        dto.setCodingRule(entity.getCodingRule());
        dto.setCycleTime(entity.getCycleTime());
        dto.setOee(entity.getOee());
        dto.setWorkerCount(entity.getWorkerCount());
        dto.setVersion(entity.getVersion());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
        dto.setDescription(entity.getDescription());
        dto.setPf(entity.getPf());
        return dto;
    }

    private ProductFamily toEntity(ProductFamilyDto dto) {
        ProductFamily entity = new ProductFamily();
        entity.setFamilyCode(dto.getFamilyCode());
        entity.setLineCode(dto.getLineCode());
        entity.setCodingRule(dto.getCodingRule());
        entity.setCycleTime(dto.getCycleTime());
        entity.setOee(dto.getOee());
        entity.setWorkerCount(dto.getWorkerCount());
        entity.setVersion(dto.getVersion());
        entity.setDescription(dto.getDescription());
        entity.setPf(dto.getPf());
        return entity;
    }
}

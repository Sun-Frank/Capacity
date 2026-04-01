package com.capics.service;

import com.capics.dto.ProductFamilyDto;
import com.capics.entity.ProductFamily;
import com.capics.entity.ProductFamilyId;
import com.capics.repository.ProductFamilyRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
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
        ProductFamilyId id = new ProductFamilyId(dto.getFamilyCode(), dto.getLineCode());
        ProductFamily entity;

        // 如果记录已存在，保留创建信息
        Optional<ProductFamily> existing = repository.findById(id);
        if (existing.isPresent()) {
            entity = existing.get();
            // 更新字段
            entity.setCodingRule(dto.getCodingRule());
            entity.setCycleTime(dto.getCycleTime());
            entity.setOee(dto.getOee());
            entity.setWorkerCount(dto.getWorkerCount());
            entity.setVersion(dto.getVersion());
            entity.setDescription(dto.getDescription());
            // 设置修改人
            entity.setUpdatedBy(dto.getUpdatedBy());
        } else {
            entity = toEntity(dto);
            entity.setCreatedBy(dto.getUpdatedBy()); // 新建时createdBy就是当前操作人
        }

        entity = repository.save(entity);
        return toDto(entity);
    }

    public void delete(String familyCode, String lineCode) {
        ProductFamilyId id = new ProductFamilyId(familyCode, lineCode);
        repository.deleteById(id);
    }

    public boolean exists(String familyCode, String lineCode) {
        ProductFamilyId id = new ProductFamilyId(familyCode, lineCode);
        return repository.existsById(id);
    }

    // 检查导入文件中的重复记录，返回已存在的记录列表
    public List<Map<String, String>> checkDuplicates(MultipartFile file) throws IOException {
        List<Map<String, String>> duplicates = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // 第一行是表头，建立列名到索引的映射
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return duplicates;

            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String header = getCellValueAsString(headerRow.getCell(i));
                if (header != null && !header.isEmpty()) {
                    headerMap.put(header.trim().toLowerCase(), i);
                }
            }

            Integer idxFamilyCode = findColumnIndex(headerMap, "familycode", "family_code", "编码族", "familyCode");
            Integer idxLineCode = findColumnIndex(headerMap, "linecode", "line_code", "生产线", "lineCode");

            if (idxFamilyCode == null || idxLineCode == null) return duplicates;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String familyCode = getCellValueAsString(row.getCell(idxFamilyCode));
                String lineCode = getCellValueAsString(row.getCell(idxLineCode));

                if (familyCode != null && !familyCode.isEmpty() && lineCode != null && !lineCode.isEmpty()) {
                    if (exists(familyCode, lineCode)) {
                        Map<String, String> dup = new HashMap<>();
                        dup.put("familyCode", familyCode);
                        dup.put("lineCode", lineCode);
                        duplicates.add(dup);
                    }
                }
            }
        }

        return duplicates;
    }

    public int importFromExcel(MultipartFile file, String createdBy) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int count = 0;

            // 第一行是表头，建立列名到索引的映射
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Excel文件缺少表头行");
            }
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String header = getCellValueAsString(headerRow.getCell(i));
                if (header != null && !header.isEmpty()) {
                    headerMap.put(header.trim().toLowerCase(), i);
                }
            }

            // 获取列索引（支持中英文表头）
            Integer idxFamilyCode = findColumnIndex(headerMap, "familycode", "family_code", "编码族", "familyCode");
            Integer idxLineCode = findColumnIndex(headerMap, "linecode", "line_code", "生产线", "lineCode");
            Integer idxCodingRule = findColumnIndex(headerMap, "codingrule", "coding_rule", "编码规则", "codingRule");
            Integer idxCycleTime = findColumnIndex(headerMap, "cycletime", "cycle_time", "周期时间", "cycleTime");
            Integer idxOee = findColumnIndex(headerMap, "oee");
            Integer idxWorkerCount = findColumnIndex(headerMap, "workercount", "worker_count", "人数", "workerCount");
            Integer idxVersion = findColumnIndex(headerMap, "version", "版本");
            Integer idxDescription = findColumnIndex(headerMap, "description", "描述", "desc");

            // 验证必需列
            if (idxFamilyCode == null || idxLineCode == null) {
                throw new RuntimeException("Excel文件缺少必需列：编码族(familyCode)或生产线(lineCode)");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                ProductFamily entity = new ProductFamily();
                entity.setFamilyCode(getCellValueAsString(row.getCell(idxFamilyCode)));
                entity.setLineCode(getCellValueAsString(row.getCell(idxLineCode)));
                entity.setCodingRule(getCellValueAsString(row.getCell(idxCodingRule)));
                entity.setCreatedBy(createdBy);

                // 周期时间
                if (idxCycleTime != null) {
                    Cell ctCell = row.getCell(idxCycleTime);
                    if (ctCell != null && ctCell.getCellType() == CellType.NUMERIC) {
                        entity.setCycleTime(BigDecimal.valueOf(ctCell.getNumericCellValue()));
                    }
                }

                // OEE
                if (idxOee != null) {
                    Cell oeeCell = row.getCell(idxOee);
                    if (oeeCell != null && oeeCell.getCellType() == CellType.NUMERIC) {
                        BigDecimal oeeValue = BigDecimal.valueOf(oeeCell.getNumericCellValue());
                        if (oeeValue.compareTo(BigDecimal.ONE) <= 0) {
                            oeeValue = oeeValue.multiply(BigDecimal.valueOf(100)).setScale(2, java.math.RoundingMode.HALF_UP);
                        }
                        entity.setOee(oeeValue);
                    }
                }

                // 人数
                if (idxWorkerCount != null) {
                    Cell workerCell = row.getCell(idxWorkerCount);
                    if (workerCell != null && workerCell.getCellType() == CellType.NUMERIC) {
                        entity.setWorkerCount((int) workerCell.getNumericCellValue());
                    }
                }

                // 版本
                if (idxVersion != null) {
                    entity.setVersion(getCellValueAsString(row.getCell(idxVersion)));
                }

                // 描述
                if (idxDescription != null) {
                    entity.setDescription(getCellValueAsString(row.getCell(idxDescription)));
                }

                if (entity.getFamilyCode() != null && !entity.getFamilyCode().isEmpty()) {
                    repository.save(entity);
                    count++;
                }
            }

            return count;
        }
    }

    // 根据多种可能的表头名称查找列索引
    private Integer findColumnIndex(Map<String, Integer> headerMap, String... possibleNames) {
        for (String name : possibleNames) {
            if (headerMap.containsKey(name.toLowerCase())) {
                return headerMap.get(name.toLowerCase());
            }
        }
        return null;
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
        return entity;
    }
}

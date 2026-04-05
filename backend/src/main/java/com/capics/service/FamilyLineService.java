package com.capics.service;

import com.capics.dto.FamilyLineDto;
import com.capics.entity.FamilyLine;
import com.capics.entity.FamilyLineId;
import com.capics.entity.ProductFamily;
import com.capics.entity.ProductFamilyId;
import com.capics.repository.FamilyLineRepository;
import com.capics.repository.ProductFamilyRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FamilyLineService {

    private static final Logger log = LoggerFactory.getLogger(FamilyLineService.class);
    private final FamilyLineRepository repository;
    private final ProductFamilyRepository productFamilyRepository;

    public FamilyLineService(FamilyLineRepository repository, ProductFamilyRepository productFamilyRepository) {
        this.repository = repository;
        this.productFamilyRepository = productFamilyRepository;
    }

    public List<FamilyLineDto> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<FamilyLineDto> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return repository.findByFamilyCodeContainingIgnoreCase(keyword.trim()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public FamilyLineDto findById(String familyCode, String lineCode) {
        FamilyLineId id = new FamilyLineId(familyCode, lineCode);
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Family line not found"));
    }

    public FamilyLineDto save(FamilyLineDto dto, String updatedBy, String originalFamilyCode, String originalLineCode) {
        log.info("FamilyLineService.save called with: originalFamilyCode={}, originalLineCode={}, new familyCode={}, new lineCode={}, updatedBy={}",
                originalFamilyCode, originalLineCode, dto.getFamilyCode(), dto.getLineCode(), updatedBy);

        // 用原始key查找记录
        FamilyLineId originalId = new FamilyLineId(originalFamilyCode, originalLineCode);
        Optional<FamilyLine> existing = repository.findById(originalId);
        log.info("findById with original key result: present={}", existing.isPresent());

        boolean keyChanged = !dto.getFamilyCode().equals(originalFamilyCode) || !dto.getLineCode().equals(originalLineCode);

        if (existing.isPresent()) {
            if (keyChanged) {
                // Key changed: delete old, create new
                log.info("Key changed, deleting old record and creating new one");
                repository.deleteById(originalId);

                FamilyLine entity = toEntity(dto);
                entity.setCreatedBy(existing.get().getCreatedBy());
                entity.setCreatedAt(existing.get().getCreatedAt());
                entity.setUpdatedBy(updatedBy);
                entity = repository.save(entity);
                return toDto(entity);
            } else {
                // Key没变，正常更新
                FamilyLine entity = existing.get();
                log.info("Updating existing FamilyLine: familyCode={}, lineCode={}",
                        entity.getFamilyCode(), entity.getLineCode());
                entity.setUpdatedBy(updatedBy);
                entity = repository.save(entity);
                return toDto(entity);
            }
        } else {
            // 记录不存在，创建新的
            log.info("Record not found, creating new");
            FamilyLine entity = toEntity(dto);
            entity.setCreatedBy(updatedBy);
            entity = repository.save(entity);
            return toDto(entity);
        }
    }

    public void delete(String familyCode, String lineCode) {
        FamilyLineId id = new FamilyLineId(familyCode, lineCode);
        repository.deleteById(id);
    }

    public boolean exists(String familyCode, String lineCode) {
        FamilyLineId id = new FamilyLineId(familyCode, lineCode);
        return repository.existsById(id);
    }

    public List<Map<String, String>> checkDuplicates(MultipartFile file) throws IOException {
        List<Map<String, String>> duplicates = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String familyCode = getCellValueAsString(row.getCell(0));
                String lineCode = getCellValueAsString(row.getCell(1));

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

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                FamilyLine entity = new FamilyLine();
                entity.setFamilyCode(getCellValueAsString(row.getCell(0)));
                entity.setLineCode(getCellValueAsString(row.getCell(1)));
                entity.setCreatedBy(createdBy);

                if (entity.getFamilyCode() != null && !entity.getFamilyCode().isEmpty()) {
                    repository.save(entity);
                    count++;
                }
            }

            return count;
        }
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

    private FamilyLineDto toDto(FamilyLine entity) {
        FamilyLineDto dto = new FamilyLineDto();
        dto.setFamilyCode(entity.getFamilyCode());
        dto.setLineCode(entity.getLineCode());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);

        // Get description from ProductFamily
        ProductFamilyId pfId = new ProductFamilyId(entity.getFamilyCode(), entity.getLineCode());
        productFamilyRepository.findById(pfId).ifPresent(pf -> dto.setDescription(pf.getDescription()));

        return dto;
    }

    private FamilyLine toEntity(FamilyLineDto dto) {
        FamilyLine entity = new FamilyLine();
        entity.setFamilyCode(dto.getFamilyCode());
        entity.setLineCode(dto.getLineCode());
        return entity;
    }
}

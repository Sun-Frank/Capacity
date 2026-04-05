package com.capics.service;

import com.capics.dto.ProductDto;
import com.capics.entity.Product;
import com.capics.entity.ProductFamily;
import com.capics.entity.ProductFamilyId;
import com.capics.repository.ProductFamilyRepository;
import com.capics.repository.ProductRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductFamilyRepository familyRepository;

    public ProductService(ProductRepository productRepository, ProductFamilyRepository familyRepository) {
        this.productRepository = productRepository;
        this.familyRepository = familyRepository;
    }

    public List<ProductDto> findAll() {
        return productRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ProductDto> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        return productRepository.findByItemNumberContainingIgnoreCase(keyword.trim()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProductDto findById(String itemNumber, String lineCode) {
        Product entity = productRepository.findById(new com.capics.entity.ProductId(itemNumber, lineCode))
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return toDto(entity);
    }

    public ProductDto save(ProductDto dto) {
        Product entity = toEntity(dto);
        entity = productRepository.save(entity);
        return toDto(entity);
    }

    public void delete(String itemNumber, String lineCode) {
        productRepository.deleteById(new com.capics.entity.ProductId(itemNumber, lineCode));
    }

    public boolean exists(String itemNumber, String lineCode) {
        return productRepository.existsById(new com.capics.entity.ProductId(itemNumber, lineCode));
    }

    // 检查导入文件中的重复记录，返回已存在的记录列表
    public List<Map<String, String>> checkDuplicates(MultipartFile file) throws IOException {
        List<Map<String, String>> duplicates = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String itemNumber = getCellValueAsString(row.getCell(0));
                String familyCode = getCellValueAsString(row.getCell(1));

                if (itemNumber != null && !itemNumber.isEmpty() && familyCode != null && !familyCode.isEmpty()) {
                    // 查找该familyCode对应的生产线
                    List<ProductFamily> families = familyRepository.findByFamilyCode(familyCode);
                    for (ProductFamily family : families) {
                        if (exists(itemNumber, family.getLineCode())) {
                            Map<String, String> dup = new HashMap<>();
                            dup.put("itemNumber", itemNumber);
                            dup.put("lineCode", family.getLineCode());
                            duplicates.add(dup);
                        }
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

                String itemNumber = getCellValueAsString(row.getCell(0));
                String familyCode = getCellValueAsString(row.getCell(1));

                if (itemNumber == null || itemNumber.isEmpty() || familyCode == null || familyCode.isEmpty()) {
                    continue;
                }

                List<ProductFamily> families = familyRepository.findByFamilyCode(familyCode);

                if (families.isEmpty()) {
                    continue;
                }

                for (ProductFamily family : families) {
                    Product product = new Product();
                    product.setItemNumber(itemNumber);
                    product.setLineCode(family.getLineCode());
                    product.setFamilyCode(familyCode);
                    product.setCycleTime(family.getCycleTime());
                    product.setOee(family.getOee());
                    product.setWorkerCount(family.getWorkerCount());
                    product.setDescription(family.getDescription());
                    product.setVersion(family.getVersion());
                    product.setCreatedBy(createdBy);

                    productRepository.save(product);
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

    private ProductDto toDto(Product entity) {
        ProductDto dto = new ProductDto();
        dto.setItemNumber(entity.getItemNumber());
        dto.setLineCode(entity.getLineCode());
        dto.setFamilyCode(entity.getFamilyCode());
        dto.setCycleTime(entity.getCycleTime());
        dto.setOee(entity.getOee());
        dto.setWorkerCount(entity.getWorkerCount());
        dto.setDescription(entity.getDescription());
        dto.setVersion(entity.getVersion());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
        // 获取PF
        if (entity.getFamilyCode() != null) {
            List<ProductFamily> families = familyRepository.findByFamilyCode(entity.getFamilyCode());
            if (!families.isEmpty()) {
                dto.setPf(families.get(0).getPf());
            }
        }
        return dto;
    }

    private Product toEntity(ProductDto dto) {
        Product entity = new Product();
        entity.setItemNumber(dto.getItemNumber());
        entity.setLineCode(dto.getLineCode());
        entity.setFamilyCode(dto.getFamilyCode());
        entity.setCycleTime(dto.getCycleTime());
        entity.setOee(dto.getOee());
        entity.setWorkerCount(dto.getWorkerCount());
        entity.setDescription(dto.getDescription());
        entity.setVersion(dto.getVersion());
        return entity;
    }
}

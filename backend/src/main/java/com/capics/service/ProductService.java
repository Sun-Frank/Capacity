package com.capics.service;

import com.capics.dto.ProductDto;
import com.capics.entity.Product;
import com.capics.entity.ProductFamily;
import com.capics.repository.ProductFamilyRepository;
import com.capics.repository.ProductRepository;
import com.capics.repository.ProjectMasterRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final String MASTER_LINE_CODE = "MASTER";

    private final ProductRepository productRepository;
    private final ProductFamilyRepository familyRepository;
    private final ProjectMasterRepository projectMasterRepository;

    public ProductService(ProductRepository productRepository,
                          ProductFamilyRepository familyRepository,
                          ProjectMasterRepository projectMasterRepository) {
        this.productRepository = productRepository;
        this.familyRepository = familyRepository;
        this.projectMasterRepository = projectMasterRepository;
    }

    public List<ProductDto> findAll() {
        return productRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Product::getItemNumber,
                        p -> p,
                        (a, b) -> compareProduct(a, b) <= 0 ? a : b,
                        LinkedHashMap::new
                ))
                .values().stream()
                .sorted(Comparator.comparing(Product::getItemNumber, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ProductDto> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String normalizedKeyword = keyword.trim();
        return productRepository
                .findByItemNumberContainingIgnoreCaseOrDescriptionContainingIgnoreCase(normalizedKeyword, normalizedKeyword)
                .stream()
                .collect(Collectors.toMap(
                        Product::getItemNumber,
                        p -> p,
                        (a, b) -> compareProduct(a, b) <= 0 ? a : b,
                        LinkedHashMap::new
                ))
                .values().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProductDto findById(String itemNumber, String lineCode) {
        Product entity = productRepository.findById(new com.capics.entity.ProductId(itemNumber, lineCode))
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return toDto(entity);
    }

    public ProductDto save(ProductDto dto) {
        if (dto.getLineCode() == null || dto.getLineCode().trim().isEmpty()) {
            dto.setLineCode(MASTER_LINE_CODE);
        }
        Product entity = productRepository.findById(new com.capics.entity.ProductId(dto.getItemNumber(), dto.getLineCode()))
                .orElseGet(Product::new);
        entity.setItemNumber(dto.getItemNumber());
        entity.setLineCode(dto.getLineCode());
        entity.setDescription(dto.getDescription());
        entity.setVersion(dto.getVersion());
        entity.setCreatedBy(entity.getCreatedBy() == null ? dto.getCreatedBy() : entity.getCreatedBy());
        entity.setUpdatedBy(dto.getUpdatedBy());
        return toDto(productRepository.save(entity));
    }

    public void delete(String itemNumber, String lineCode) {
        productRepository.deleteById(new com.capics.entity.ProductId(itemNumber, lineCode));
    }

    public boolean exists(String itemNumber, String lineCode) {
        return productRepository.existsById(new com.capics.entity.ProductId(itemNumber, lineCode));
    }

    public List<Map<String, String>> checkDuplicates(MultipartFile file) throws IOException {
        List<Map<String, String>> duplicates = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String itemNumber = getCellValueAsString(row.getCell(0));
                if (itemNumber != null && !itemNumber.isEmpty() && exists(itemNumber.trim(), MASTER_LINE_CODE)) {
                    Map<String, String> dup = new HashMap<>();
                    dup.put("itemNumber", itemNumber.trim());
                    dup.put("lineCode", MASTER_LINE_CODE);
                    duplicates.add(dup);
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
                String description = getCellValueAsString(row.getCell(1));
                if (itemNumber == null || itemNumber.trim().isEmpty()) {
                    continue;
                }
                Product product = productRepository.findById(new com.capics.entity.ProductId(itemNumber.trim(), MASTER_LINE_CODE))
                        .orElseGet(Product::new);
                product.setItemNumber(itemNumber.trim());
                product.setLineCode(MASTER_LINE_CODE);
                product.setDescription(description == null ? null : description.trim());
                product.setCreatedBy(product.getCreatedBy() == null ? createdBy : product.getCreatedBy());
                product.setUpdatedBy(createdBy);
                productRepository.save(product);
                count++;
            }
            return count;
        }
    }

    public byte[] buildTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("成品料号*");
            header.createCell(1).setCellValue("产品描述");
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double number = cell.getNumericCellValue();
                if (number == Math.rint(number)) {
                    return String.valueOf((long) number);
                }
                return String.valueOf(number);
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
        dto.setDescriptionExistsInProjectMaster(entity.getDescription() != null
                && projectMasterRepository.existsByProductDescriptionIgnoreCase(entity.getDescription()));
        if (entity.getFamilyCode() != null) {
            List<ProductFamily> families = familyRepository.findByFamilyCode(entity.getFamilyCode());
            if (!families.isEmpty()) {
                dto.setPf(families.get(0).getPf());
            }
        }
        return dto;
    }

    private int compareProduct(Product a, Product b) {
        if (MASTER_LINE_CODE.equalsIgnoreCase(a.getLineCode())) return -1;
        if (MASTER_LINE_CODE.equalsIgnoreCase(b.getLineCode())) return 1;
        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
        if (a.getCreatedAt() == null) return 1;
        if (b.getCreatedAt() == null) return -1;
        return a.getCreatedAt().compareTo(b.getCreatedAt());
    }
}

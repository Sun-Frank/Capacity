package com.capics.service;

import com.capics.dto.ProjectMasterDto;
import com.capics.entity.ProjectMaster;
import com.capics.repository.ProjectMasterRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ProjectMasterService {

    private final ProjectMasterRepository repository;
    private final DataFormatter formatter = new DataFormatter();

    public ProjectMasterService(ProjectMasterRepository repository) {
        this.repository = repository;
    }

    public List<ProjectMasterDto> findAll(String keyword) {
        List<ProjectMaster> rows = keyword == null || keyword.trim().isEmpty()
                ? repository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                : repository.search(keyword.trim());
        return rows.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ProjectMasterDto save(ProjectMasterDto dto, String operator) {
        ProjectMaster entity = dto.getId() == null
                ? new ProjectMaster()
                : repository.findById(dto.getId()).orElse(new ProjectMaster());
        entity.setCustomer(trim(dto.getCustomer()));
        entity.setProductPlatform(trim(dto.getProductPlatform()));
        entity.setVehicleConfig(trim(dto.getVehicleConfig()));
        entity.setProductDescription(required(dto.getProductDescription(), "productDescription"));
        entity.setBws(trim(dto.getBws()));
        entity.setVersion(trim(dto.getVersion()));
        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy(operator);
        }
        entity.setUpdatedBy(operator);
        return toDto(repository.save(entity));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public int importFromExcel(MultipartFile file, String operator) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) throw new IOException("Project master sheet not found");
            int count = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String productDescription = read(row, 3);
                if (isBlank(productDescription)) continue;
                ProjectMasterDto dto = new ProjectMasterDto();
                dto.setCustomer(read(row, 0));
                dto.setProductPlatform(read(row, 1));
                dto.setVehicleConfig(read(row, 2));
                dto.setProductDescription(productDescription);
                dto.setBws(read(row, 4));
                dto.setVersion(read(row, 5));
                save(dto, operator);
                count++;
            }
            return count;
        }
    }

    public byte[] buildTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Data");
            Row header = sheet.createRow(0);
            String[] headers = {"客户", "产品平台", "车型配置", "产品描述*", "BWS", "版本"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private ProjectMasterDto toDto(ProjectMaster entity) {
        ProjectMasterDto dto = new ProjectMasterDto();
        dto.setId(entity.getId());
        dto.setCustomer(entity.getCustomer());
        dto.setProductPlatform(entity.getProductPlatform());
        dto.setVehicleConfig(entity.getVehicleConfig());
        dto.setProductDescription(entity.getProductDescription());
        dto.setBws(entity.getBws());
        dto.setVersion(entity.getVersion());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString());
        return dto;
    }

    private String read(Row row, int index) {
        return row.getCell(index) == null ? null : trim(formatter.formatCellValue(row.getCell(index)));
    }

    private String required(String value, String field) {
        if (isBlank(value)) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

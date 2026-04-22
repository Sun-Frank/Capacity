package com.capics.service;

import com.capics.dto.CtLineDataDto;
import com.capics.entity.CtLineData;
import com.capics.repository.CtLineDataRepository;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CtLineDataService {

    private static final String KEY_B = "colB";
    private static final String KEY_C = "colC";
    private static final String KEY_D = "colD";
    private static final String KEY_F = "colF";
    private static final String KEY_I = "colI";
    private static final String KEY_P = "colP";
    private static final String KEY_W = "colW";
    private static final String KEY_X = "colX";

    private final CtLineDataRepository repository;
    private final DataFormatter dataFormatter = new DataFormatter();
    @PersistenceContext
    private EntityManager entityManager;

    public CtLineDataService(CtLineDataRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> getCtLinePageData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("headers", fixedHeaders());
        data.put("rows", findAllRows());
        return data;
    }

    public int importFromExcel(MultipartFile file, String createdBy) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IOException("导入失败：未找到工作表");
            }

            Row header = sheet.getRow(0);
            if (header == null) {
                throw new IOException("导入失败：第1行表头不存在");
            }

            List<CtLineData> toSave = new ArrayList<>();
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    continue;
                }

                String colB = readCellAsString(row, 1);
                String colC = readCellAsString(row, 2);
                String colD = readCellAsString(row, 3);
                String colF = readCellAsString(row, 5);
                String colI = readCellAsString(row, 8);
                String colP = readCellAsString(row, 15);
                String colW = readCellAsString(row, 22);
                String colX = readCellAsString(row, 23);

                if (isBlank(colB) && isBlank(colC) && isBlank(colD) && isBlank(colF)
                        && isBlank(colI) && isBlank(colP) && isBlank(colW) && isBlank(colX)) {
                    continue;
                }

                validateRequired(colB, colC, colD, colF, colI, colP, rowNum + 1);

                CtLineData entity = new CtLineData();
                entity.setColB(colB.trim());
                entity.setColC(colC.trim());
                entity.setColD(colD.trim());
                entity.setColF(colF.trim());
                entity.setColI(colI.trim());
                entity.setColP(colP.trim());
                entity.setColW(isBlank(colW) ? null : colW.trim());
                entity.setColX(isBlank(colX) ? null : colX.trim());
                entity.setCreatedBy(createdBy);
                entity.setUpdatedBy(createdBy);
                toSave.add(entity);
            }

            repository.saveAll(toSave);
            return toSave.size();
        }
    }

    @Transactional(timeout = 12)
    public CtLineDataDto updateById(Long id, CtLineDataDto input, String updatedBy) throws IOException {
        applyWriteTimeout();
        CtLineData entity = repository.findById(id)
                .orElseThrow(() -> new IOException("未找到数据: id=" + id));

        validateRequired(input.getColB(), input.getColC(), input.getColD(), input.getColF(), input.getColI(), input.getColP(), null);

        entity.setColB(input.getColB().trim());
        entity.setColC(input.getColC().trim());
        entity.setColD(input.getColD().trim());
        entity.setColF(input.getColF().trim());
        entity.setColI(input.getColI().trim());
        entity.setColP(input.getColP().trim());
        entity.setColW(isBlank(input.getColW()) ? null : input.getColW().trim());
        entity.setColX(isBlank(input.getColX()) ? null : input.getColX().trim());
        entity.setUpdatedBy(updatedBy);

        CtLineData saved = repository.saveAndFlush(entity);
        return toDto(saved);
    }

    public byte[] buildTemplateFile() throws IOException {
        Map<String, String> headers = fixedHeaders();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Data");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue(headers.get(KEY_B));
            headerRow.createCell(1).setCellValue(headers.get(KEY_C));
            headerRow.createCell(2).setCellValue(headers.get(KEY_D));
            headerRow.createCell(3).setCellValue(headers.get(KEY_F));
            headerRow.createCell(4).setCellValue(headers.get(KEY_I));
            headerRow.createCell(5).setCellValue(headers.get(KEY_P));
            headerRow.createCell(6).setCellValue(headers.get(KEY_W));
            headerRow.createCell(7).setCellValue(headers.get(KEY_X));

            for (int i = 0; i < 8; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void validateRequired(String colB, String colC, String colD, String colF, String colI, String colP, Integer rowNo) throws IOException {
        List<String> missing = new ArrayList<>();
        if (isBlank(colB)) missing.add("生产线");
        if (isBlank(colC)) missing.add("物料号");
        if (isBlank(colD)) missing.add("主备线");
        if (isBlank(colF)) missing.add("CT(秒)");
        if (isBlank(colI)) missing.add("OEE");
        if (isBlank(colP)) missing.add("人数");
        if (!missing.isEmpty()) {
            String prefix = rowNo == null ? "" : ("第" + rowNo + "行");
            throw new IOException(prefix + "必填列不能为空: " + String.join(", ", missing));
        }
    }

    private Map<String, String> fixedHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(KEY_B, "生产线");
        headers.put(KEY_C, "物料号");
        headers.put(KEY_D, "主备线");
        headers.put(KEY_F, "CT(秒)");
        headers.put(KEY_I, "OEE");
        headers.put(KEY_P, "人数");
        headers.put(KEY_W, "最后修改日期");
        headers.put(KEY_X, "最后修改人");
        return headers;
    }

    private List<CtLineDataDto> findAllRows() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private String readCellAsString(Row row, int index) {
        if (row.getCell(index) == null) {
            return null;
        }
        String raw = dataFormatter.formatCellValue(row.getCell(index));
        return raw == null ? null : raw.trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void applyWriteTimeout() {
        entityManager.createNativeQuery("SET LOCAL lock_timeout = '5s'").executeUpdate();
        entityManager.createNativeQuery("SET LOCAL statement_timeout = '10s'").executeUpdate();
    }

    private CtLineDataDto toDto(CtLineData entity) {
        CtLineDataDto dto = new CtLineDataDto();
        dto.setId(entity.getId());
        dto.setColB(entity.getColB());
        dto.setColC(entity.getColC());
        dto.setColD(entity.getColD());
        dto.setColF(entity.getColF());
        dto.setColI(entity.getColI());
        dto.setColP(entity.getColP());
        dto.setColW(entity.getColW());
        dto.setColX(entity.getColX());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
        return dto;
    }
}

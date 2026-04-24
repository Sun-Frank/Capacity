package com.capics.service;

import com.capics.dto.CtLineDataDto;
import com.capics.dto.CtLineImportTaskDto;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CtLineDataRepository repository;
    private final DataFormatter dataFormatter = new DataFormatter();
    private final Map<String, ImportTaskState> importTasks = new ConcurrentHashMap<>();

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
        return doImport(file.getInputStream(), createdBy, null);
    }

    public CtLineImportTaskDto startImportTask(MultipartFile file, String createdBy) throws IOException {
        byte[] fileBytes = file.getBytes();
        String taskId = UUID.randomUUID().toString();

        ImportTaskState state = new ImportTaskState(taskId);
        state.status = "RUNNING";
        state.message = "Task started";
        importTasks.put(taskId, state);

        CompletableFuture.runAsync(() -> {
            try {
                int count = doImport(new ByteArrayInputStream(fileBytes), createdBy, state);
                state.importedCount = count;
                state.progress = 100;
                state.status = "SUCCESS";
                state.message = "Import success, count=" + count;
            } catch (Exception ex) {
                state.status = "FAILED";
                state.error = ex.getMessage();
                state.message = "Import failed";
            } finally {
                state.finishedAt = LocalDateTime.now();
            }
        });

        return toTaskDto(state);
    }

    public CtLineImportTaskDto getImportTask(String taskId) throws IOException {
        ImportTaskState state = importTasks.get(taskId);
        if (state == null) {
            throw new IOException("Import task not found: " + taskId);
        }
        return toTaskDto(state);
    }

    private int doImport(InputStream inputStream, String createdBy, ImportTaskState taskState) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IOException("Import failed: worksheet not found");
            }

            Row header = sheet.getRow(0);
            if (header == null) {
                throw new IOException("Import failed: header row is missing");
            }

            int lastRowNum = sheet.getLastRowNum();
            int totalRows = Math.max(lastRowNum, 0);
            if (taskState != null) {
                taskState.totalRows = totalRows;
                taskState.processedRows = 0;
                taskState.progress = 0;
                taskState.message = "Parsing workbook";
            }

            List<CtLineData> toSave = new ArrayList<>();
            Map<String, CtLineData> existingByLineItem = new LinkedHashMap<>();
            for (CtLineData existing : repository.findAll()) {
                existingByLineItem.put(composeLineItemKey(existing.getColB(), existing.getColC()), existing);
            }
            for (int rowNum = 1; rowNum <= lastRowNum; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) {
                    updateTaskProgress(taskState, rowNum, totalRows);
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
                    updateTaskProgress(taskState, rowNum, totalRows);
                    continue;
                }

                validateRequired(colB, colC, colD, colF, colI, colP, rowNum + 1);

                String normalizedColB = colB.trim();
                String normalizedColC = colC.trim();
                String lineItemKey = composeLineItemKey(normalizedColB, normalizedColC);

                CtLineData entity = existingByLineItem.get(lineItemKey);
                if (entity == null) {
                    entity = new CtLineData();
                    entity.setColB(normalizedColB);
                    entity.setColC(normalizedColC);
                    entity.setCreatedBy(createdBy);
                    existingByLineItem.put(lineItemKey, entity);
                }

                entity.setColD(colD.trim());
                entity.setColF(colF.trim());
                entity.setColI(colI.trim());
                entity.setColP(colP.trim());
                entity.setColW(isBlank(colW) ? null : colW.trim());
                entity.setColX(isBlank(colX) ? null : colX.trim());
                entity.setUpdatedBy(createdBy);
                toSave.add(entity);

                updateTaskProgress(taskState, rowNum, totalRows);
            }

            if (taskState != null) {
                taskState.message = "Writing to database";
                taskState.progress = 95;
            }
            repository.saveAll(existingByLineItem.values());
            return toSave.size();
        }
    }

    @Transactional(timeout = 12)
    public CtLineDataDto create(CtLineDataDto input, String createdBy) throws IOException {
        applyWriteTimeout();
        validateRequired(input.getColB(), input.getColC(), input.getColD(), input.getColF(), input.getColI(), input.getColP(), null);

        CtLineData entity = new CtLineData();
        entity.setColB(input.getColB().trim());
        entity.setColC(input.getColC().trim());
        entity.setColD(input.getColD().trim());
        entity.setColF(input.getColF().trim());
        entity.setColI(input.getColI().trim());
        entity.setColP(input.getColP().trim());
        entity.setColW(isBlank(input.getColW()) ? null : input.getColW().trim());
        entity.setColX(isBlank(input.getColX()) ? null : input.getColX().trim());
        entity.setCreatedBy(createdBy);
        entity.setUpdatedBy(createdBy);

        CtLineData saved = repository.saveAndFlush(entity);
        return toDto(saved);
    }

    @Transactional(timeout = 12)
    public CtLineDataDto updateById(Long id, CtLineDataDto input, String updatedBy) throws IOException {
        applyWriteTimeout();
        CtLineData entity = repository.findById(id)
                .orElseThrow(() -> new IOException("Data not found: id=" + id));

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
        if (isBlank(colB)) missing.add("line_code");
        if (isBlank(colC)) missing.add("item_number");
        if (isBlank(colD)) missing.add("main_or_backup");
        if (isBlank(colF)) missing.add("ct_seconds");
        if (isBlank(colI)) missing.add("oee");
        if (isBlank(colP)) missing.add("headcount");
        if (!missing.isEmpty()) {
            String prefix = rowNo == null ? "" : ("row " + rowNo + " ");
            throw new IOException(prefix + "required columns are empty: " + String.join(", ", missing));
        }
    }

    private Map<String, String> fixedHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(KEY_B, "\u751f\u4ea7\u7ebf");
        headers.put(KEY_C, "\u7269\u6599\u53f7");
        headers.put(KEY_D, "\u4e3b\u5907\u7ebf");
        headers.put(KEY_F, "CT(\u79d2)");
        headers.put(KEY_I, "OEE");
        headers.put(KEY_P, "\u4eba\u6570");
        headers.put(KEY_W, "\u6700\u540e\u4fee\u6539\u65e5\u671f");
        headers.put(KEY_X, "\u6700\u540e\u4fee\u6539\u4eba");
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

    private void updateTaskProgress(ImportTaskState taskState, int processedRows, int totalRows) {
        if (taskState == null) {
            return;
        }
        taskState.processedRows = processedRows;
        if (totalRows <= 0) {
            taskState.progress = 90;
            return;
        }
        int progress = (int) Math.floor((processedRows * 90.0) / totalRows);
        taskState.progress = Math.min(90, Math.max(0, progress));
    }

    private String composeLineItemKey(String colB, String colC) {
        return (colB == null ? "" : colB.trim()) + "||" + (colC == null ? "" : colC.trim());
    }

    private CtLineImportTaskDto toTaskDto(ImportTaskState state) {
        CtLineImportTaskDto dto = new CtLineImportTaskDto();
        dto.setTaskId(state.taskId);
        dto.setStatus(state.status);
        dto.setProgress(state.progress);
        dto.setTotalRows(state.totalRows);
        dto.setProcessedRows(state.processedRows);
        dto.setImportedCount(state.importedCount);
        dto.setMessage(state.message);
        dto.setError(state.error);
        dto.setStartedAt(state.startedAt != null ? state.startedAt.format(TIME_FORMATTER) : null);
        dto.setFinishedAt(state.finishedAt != null ? state.finishedAt.format(TIME_FORMATTER) : null);
        return dto;
    }

    private static class ImportTaskState {
        private final String taskId;
        private volatile String status;
        private volatile int progress;
        private volatile Integer totalRows;
        private volatile Integer processedRows;
        private volatile Integer importedCount;
        private volatile String message;
        private volatile String error;
        private final LocalDateTime startedAt;
        private volatile LocalDateTime finishedAt;

        private ImportTaskState(String taskId) {
            this.taskId = taskId;
            this.status = "PENDING";
            this.progress = 0;
            this.startedAt = LocalDateTime.now();
        }
    }
}

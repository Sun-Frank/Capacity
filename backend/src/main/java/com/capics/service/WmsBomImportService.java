package com.capics.service;

import com.capics.dto.WmsBomImportRequest;
import com.capics.dto.WmsBomImportResult;
import com.capics.dto.WmsBomRow;
import com.capics.entity.WmsBomConfig;
import com.capics.repository.MrpPlanRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class WmsBomImportService {
    private final WmsBomConfigService configService;
    private final MrpPlanRepository mrpPlanRepository;
    private final RoutingService routingService;
    private final ObjectMapper objectMapper;
    private final String pythonCommand;
    private final long timeoutMinutes;

    public WmsBomImportService(WmsBomConfigService configService,
                               MrpPlanRepository mrpPlanRepository,
                               RoutingService routingService,
                               ObjectMapper objectMapper,
                               @Value("${wms.bom.python-command:${WMS_BOM_PYTHON:python3}}") String pythonCommand,
                               @Value("${wms.bom.timeout-minutes:${WMS_BOM_TIMEOUT_MINUTES:45}}") long timeoutMinutes) {
        this.configService = configService;
        this.mrpPlanRepository = mrpPlanRepository;
        this.routingService = routingService;
        this.objectMapper = objectMapper;
        this.pythonCommand = pythonCommand;
        this.timeoutMinutes = timeoutMinutes;
    }

    public WmsBomImportResult fetchAndImport(WmsBomImportRequest request, String operator) throws IOException, InterruptedException {
        String createdBy = required(request.getCreatedBy(), "createdBy");
        String fileName = required(request.getFileName(), "fileName");
        String version = required(request.getVersion(), "version");
        boolean overwrite = request.getOverwrite() == null || request.getOverwrite();

        List<String> materials = mrpPlanRepository.findDistinctItemNumbersByCreatedByAndFileNameAndVersion(createdBy, fileName, version);
        Set<String> normalizedMaterials = new LinkedHashSet<>();
        for (String material : materials) {
            String trimmed = trimToNull(material);
            if (trimmed != null) {
                normalizedMaterials.add(trimmed);
            }
        }
        if (normalizedMaterials.isEmpty()) {
            throw new IllegalArgumentException("No MRP item numbers found for selected createdBy/fileName/version");
        }

        WmsBomConfig config = configService.getRuntimeConfig();
        Path workDir = Files.createTempDirectory("capics-wms-bom-");
        try {
            Path configJson = workDir.resolve("wms-config.json");
            Path materialsFile = workDir.resolve("materials.txt");
            Path outputFile = workDir.resolve("bom-routing.xlsx");
            Path scriptFile = workDir.resolve("wms_bom_fetch.py");

            Map<String, Object> configPayload = new LinkedHashMap<>();
            configPayload.put("loginUrl", config.getLoginUrl());
            configPayload.put("username", config.getUsername());
            configPayload.put("password", config.getPassword());
            objectMapper.writeValue(configJson.toFile(), configPayload);
            Files.write(materialsFile, normalizedMaterials, StandardCharsets.UTF_8);
            copyScript(scriptFile);

            runPythonFetcher(scriptFile, configJson, materialsFile, outputFile, workDir);

            List<WmsBomRow> rows = readRows(outputFile);
            RoutingService.ImportSummary summary = routingService.importWmsRows(rows, operator, overwrite);

            WmsBomImportResult result = new WmsBomImportResult();
            result.setMaterialCount(normalizedMaterials.size());
            result.setProductCount(summary.getProductCount());
            result.setItemCount(summary.getItemCount());
            result.setMessage("Imported WMS BOM data");
            return result;
        } finally {
            deleteQuietly(workDir);
        }
    }

    private void runPythonFetcher(Path scriptFile, Path configJson, Path materialsFile, Path outputFile, Path workDir)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                pythonCommand,
                scriptFile.toString(),
                "--config", configJson.toString(),
                "--materials", materialsFile.toString(),
                "--output", outputFile.toString(),
                "--work-dir", workDir.toString()
        );
        builder.redirectErrorStream(true);
        Path logFile = workDir.resolve("wms-fetch.log");
        builder.redirectOutput(logFile.toFile());
        Process process = builder.start();
        boolean finished = process.waitFor(Duration.ofMinutes(timeoutMinutes).toMillis(), TimeUnit.MILLISECONDS);
        String output = Files.exists(logFile) ? new String(Files.readAllBytes(logFile), StandardCharsets.UTF_8) : "";
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("WMS BOM fetch timed out after " + timeoutMinutes + " minutes");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("WMS BOM fetch failed: " + output);
        }
        if (!Files.exists(outputFile)) {
            throw new IllegalStateException("WMS BOM fetch did not produce output file: " + output);
        }
    }

    private List<WmsBomRow> readRows(Path outputFile) throws IOException {
        List<WmsBomRow> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(outputFile))) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() <= 1) {
                return rows;
            }
            Map<String, Integer> headers = readHeaders(sheet.getRow(0), formatter);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                WmsBomRow item = new WmsBomRow();
                item.setProductNumber(cell(row, headers, formatter, "成品物料号"));
                item.setComponentNumber(cell(row, headers, formatter, "组件物料号"));
                item.setLineCode(cell(row, headers, formatter, "生产线"));
                item.setBomLevel(parseInteger(cell(row, headers, formatter, "BOM层级")));
                item.setBomQuantity(parseDecimal(cell(row, headers, formatter, "BOM用量")));
                if (!isBlank(item.getProductNumber()) && !isBlank(item.getComponentNumber()) && item.getBomLevel() != null) {
                    rows.add(item);
                }
            }
        }
        return rows;
    }

    private Map<String, Integer> readHeaders(Row row, DataFormatter formatter) {
        Map<String, Integer> headers = new LinkedHashMap<>();
        if (row == null) {
            return headers;
        }
        for (Cell cell : row) {
            headers.put(formatter.formatCellValue(cell).trim(), cell.getColumnIndex());
        }
        return headers;
    }

    private String cell(Row row, Map<String, Integer> headers, DataFormatter formatter, String name) {
        Integer index = headers.get(name);
        if (index == null) {
            return null;
        }
        return trimToNull(formatter.formatCellValue(row.getCell(index)));
    }

    private void copyScript(Path target) throws IOException {
        ClassPathResource resource = new ClassPathResource("scripts/wms_bom_fetch.py");
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, target);
        }
    }

    private String required(String value, String name) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return trimmed;
    }

    private Integer parseInteger(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim()).intValue();
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String value) {
        if (isBlank(value)) {
            return BigDecimal.ONE;
        }
        try {
            return new BigDecimal(value.trim().replace(",", ""));
        } catch (Exception ex) {
            return BigDecimal.ONE;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void deleteQuietly(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            stream
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(item -> {
                        try {
                            Files.deleteIfExists(item);
                        } catch (IOException ignored) {
                            // Best-effort cleanup of temporary WMS work files.
                        }
                    });
        } catch (IOException ignored) {
            // Best-effort cleanup of temporary WMS work files.
        }
    }
}

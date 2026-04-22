package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.MrpPlanDto;
import com.capics.service.MrpPlanService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@RestController
@RequestMapping("/api/mrp")
public class MrpController {

    private final MrpPlanService mrpPlanService;

    public MrpController(MrpPlanService mrpPlanService) {
        this.mrpPlanService = mrpPlanService;
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse> getAllPlans() {
        List<MrpPlanDto> plans = mrpPlanService.findAll();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @GetMapping("/plans/{id}")
    public ResponseEntity<ApiResponse> getPlanById(@PathVariable Long id) {
        MrpPlanDto plan = mrpPlanService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @GetMapping("/plans/version/{version}")
    public ResponseEntity<ApiResponse> getPlansByVersion(@PathVariable String version) {
        List<MrpPlanDto> plans = mrpPlanService.findByVersion(version);
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @GetMapping("/versions")
    public ResponseEntity<ApiResponse> getAllVersions() {
        List<String> versions = mrpPlanService.getAllVersions();
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    // 鑾峰彇鏈€鏂板鍏ョ殑MRP鏂囦欢淇℃伅
    @GetMapping("/latest-file")
    public ResponseEntity<ApiResponse> getLatestMrpFile() {
        Map<String, String> fileInfo = mrpPlanService.getLatestMrpFileInfo();
        return ResponseEntity.ok(ApiResponse.success(fileInfo));
    }

    @PostMapping("/plans")
    public ResponseEntity<ApiResponse> createPlan(@RequestBody MrpPlanDto dto) {
        MrpPlanDto saved = mrpPlanService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Created", saved));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<ApiResponse> updatePlan(@PathVariable Long id, @RequestBody MrpPlanDto dto) {
        dto.setId(id);
        MrpPlanDto updated = mrpPlanService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Updated", updated));
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<ApiResponse> deletePlan(@PathVariable Long id) {
        mrpPlanService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }

    @PostMapping("/plans/import")
    public ResponseEntity<ApiResponse> importPlans(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "createdBy", required = false) String createdBy,
            Principal principal) {
        try {
            String effectiveCreatedBy = createdBy;
            if (effectiveCreatedBy == null || effectiveCreatedBy.trim().isEmpty()) {
                effectiveCreatedBy = principal != null ? principal.getName() : "unknown";
            }
            int count = mrpPlanService.importFromExcel(file, fileName, effectiveCreatedBy.trim());
            return ResponseEntity.ok(ApiResponse.success("Imported " + count + " records"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Import failed: " + e.getMessage()));
        }
    }

    @GetMapping("/plans/template")
    public ResponseEntity<Resource> downloadMrpTemplate() throws IOException {
        String fileName = "MRP瀵煎叆妯℃澘-v2.xlsx";
        File localFile = new File("import_templates", fileName);
        Resource resource;
        if (localFile.exists()) {
            resource = new org.springframework.core.io.FileSystemResource(localFile);
        } else {
            resource = new ClassPathResource("import_templates/" + fileName);
        }
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // Filter endpoints for three-level cascade filtering
    @GetMapping("/filters/created-bys")
    public ResponseEntity<ApiResponse> getAllCreatedBys() {
        List<String> createdBys = mrpPlanService.getAllCreatedBys();
        return ResponseEntity.ok(ApiResponse.success(createdBys));
    }

    @GetMapping("/filters/{createdBy}/files")
    public ResponseEntity<ApiResponse> getFileNamesByCreatedBy(@PathVariable String createdBy) {
        List<String> fileNames = mrpPlanService.getFileNamesByCreatedBy(createdBy);
        return ResponseEntity.ok(ApiResponse.success(fileNames));
    }

    @GetMapping("/filters/{createdBy}/{fileName}/versions")
    public ResponseEntity<ApiResponse> getVersionsByCreatedByAndFileName(
            @PathVariable String createdBy, @PathVariable String fileName) {
        List<String> versions = mrpPlanService.getVersionsByCreatedByAndFileName(createdBy, fileName);
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    @GetMapping("/plans/filtered")
    public ResponseEntity<ApiResponse> getPlansFiltered(
            @RequestParam String createdBy,
            @RequestParam String fileName,
            @RequestParam String version) {
        List<MrpPlanDto> plans = mrpPlanService.findByCreatedByAndFileNameAndVersion(createdBy, fileName, version);
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @GetMapping("/plans/by-file")
    public ResponseEntity<ApiResponse> getPlansByFile(
            @RequestParam String createdBy,
            @RequestParam String fileName) {
        List<MrpPlanDto> plans = mrpPlanService.findByCreatedByAndFileName(createdBy, fileName);
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @GetMapping("/reports/weekly")
    public ResponseEntity<ApiResponse> getWeeklyReport(@RequestParam String version) {
        List<Map<String, Object>> report = mrpPlanService.getWeeklyReport(version);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // 鍛ㄦ姤琛?- 鎸夊鍏ヤ汉+鏂囦欢鍚嶏紝澶氱増鏈姣?
    @GetMapping("/reports/weekly/by-file")
    public ResponseEntity<ApiResponse> getWeeklyReportByFile(
            @RequestParam String createdBy,
            @RequestParam String fileName) {
        List<Map<String, Object>> report = mrpPlanService.getWeeklyReportByCreatedByAndFileName(createdBy, fileName);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // 鏈堟姤琛?- 鎸夊鍏ヤ汉+鏂囦欢鍚嶏紝澶氱増鏈姣?
    @GetMapping("/reports/monthly/by-file")
    public ResponseEntity<ApiResponse> getMonthlyReportByFile(
            @RequestParam String createdBy,
            @RequestParam String fileName) {
        List<Map<String, Object>> report = mrpPlanService.getMonthlyReportByCreatedByAndFileName(createdBy, fileName);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // 鍗曠増鏈懆闇€姹傛眹鎬?- 鎸夊鍏ヤ汉+鏂囦欢鍚?鐗堟湰
    @GetMapping("/reports/weekly/single")
    public ResponseEntity<ApiResponse> getWeeklyDemandSingle(
            @RequestParam String createdBy,
            @RequestParam String fileName,
            @RequestParam String version) {
        Map<String, Object> report = mrpPlanService.getWeeklyDemandByVersion(createdBy, fileName, version);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // 鍗曠増鏈湀闇€姹傛眹鎬?- 鎸夊鍏ヤ汉+鏂囦欢鍚?鐗堟湰
    @GetMapping("/reports/monthly/single")
    public ResponseEntity<ApiResponse> getMonthlyDemandSingle(
            @RequestParam String createdBy,
            @RequestParam String fileName,
            @RequestParam String version) {
        Map<String, Object> report = mrpPlanService.getMonthlyDemandByVersion(createdBy, fileName, version);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/reports/monthly")
    public ResponseEntity<ApiResponse> getMonthlyReport(@RequestParam String version) {
        List<Map<String, Object>> report = mrpPlanService.getMonthlyReport(version);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/reports/weekly/export")
    public ResponseEntity<byte[]> exportWeeklyReport(
            @RequestParam String createdBy,
            @RequestParam String fileName) throws IOException {
        List<Map<String, Object>> report = mrpPlanService.getWeeklyReportByCreatedByAndFileName(createdBy, fileName);
        String outputFileName = "MRP-weekly-report-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        return buildExportResponse(report, outputFileName, true);
    }

    @GetMapping("/reports/monthly/export")
    public ResponseEntity<byte[]> exportMonthlyReport(
            @RequestParam String createdBy,
            @RequestParam String fileName) throws IOException {
        List<Map<String, Object>> report = mrpPlanService.getMonthlyReportByCreatedByAndFileName(createdBy, fileName);
        String outputFileName = "MRP-monthly-report-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        return buildExportResponse(report, outputFileName, false);
    }

    private ResponseEntity<byte[]> buildExportResponse(
            List<Map<String, Object>> report, String fileName, boolean weekly) throws IOException {
        byte[] bytes = buildReportWorkbook(report, weekly);
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @SuppressWarnings("unchecked")
    private byte[] buildReportWorkbook(List<Map<String, Object>> report, boolean weekly) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(weekly ? "Weekly Report" : "Monthly Report");

            if (report == null || report.isEmpty()) {
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Item Number");
                header.createCell(1).setCellValue("Description");
                workbook.write(output);
                return output.toByteArray();
            }

            Map<String, Object> wrapper = report.get(0);
            List<Map<String, String>> columns = (List<Map<String, String>>) wrapper.get("columns");
            List<Map<String, Object>> data = (List<Map<String, Object>>) wrapper.get("data");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Item Number");
            header.createCell(1).setCellValue("Description");
            for (int i = 0; i < columns.size(); i++) {
                Map<String, String> col = columns.get(i);
                String groupLabel = weekly ? col.get("weekLabel") : col.get("monthLabel");
                String title = (groupLabel == null ? col.get("key") : groupLabel) + " | " + col.get("version");
                header.createCell(i + 2).setCellValue(title);
            }

            int rowIndex = 1;
            for (Map<String, Object> rowData : data) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(stringValue(rowData.get("itemNumber")));
                row.createCell(1).setCellValue(stringValue(rowData.get("description")));

                for (int i = 0; i < columns.size(); i++) {
                    String key = columns.get(i).get("key");
                    Object value = rowData.get(key);
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof Number) {
                        row.createCell(i + 2, CellType.NUMERIC).setCellValue(((Number) value).doubleValue());
                    } else {
                        row.createCell(i + 2).setCellValue(value.toString());
                    }
                }
            }

            for (int i = 0; i < columns.size() + 2; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}

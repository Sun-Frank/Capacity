package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.LineConfigDto;
import com.capics.service.LineConfigService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

@RestController
@RequestMapping("/api/lines")
public class LineController {

    private final LineConfigService lineConfigService;

    public LineController(LineConfigService lineConfigService) {
        this.lineConfigService = lineConfigService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAll() {
        List<LineConfigDto> lines = lineConfigService.findAll();
        return ResponseEntity.ok(ApiResponse.success(lines));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse> getActive() {
        List<LineConfigDto> lines = lineConfigService.findActive();
        return ResponseEntity.ok(ApiResponse.success(lines));
    }

    @GetMapping("/{lineCode}")
    public ResponseEntity<ApiResponse> getById(@PathVariable String lineCode) {
        LineConfigDto line = lineConfigService.findById(lineCode);
        return ResponseEntity.ok(ApiResponse.success(line));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody LineConfigDto dto) {
        LineConfigDto saved = lineConfigService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Created", saved));
    }

    @PutMapping("/{lineCode}")
    public ResponseEntity<ApiResponse> update(@PathVariable String lineCode, @RequestBody LineConfigDto dto) {
        dto.setLineCode(lineCode);
        LineConfigDto updated = lineConfigService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Updated", updated));
    }

    @DeleteMapping("/{lineCode}")
    public ResponseEntity<ApiResponse> delete(@PathVariable String lineCode) {
        lineConfigService.delete(lineCode);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse> importLines(@RequestParam("file") MultipartFile file, Principal principal) {
        try {
            String updatedBy = principal != null ? principal.getName() : "system";
            int count = lineConfigService.importFromExcel(file, updatedBy);
            return ResponseEntity.ok(ApiResponse.success("Imported " + count + " line records"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Import line configs failed: " + e.getMessage()));
        }
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() throws IOException {
        String displayFileName = "\u751f\u4ea7\u7ebf\u914d\u7f6e\u5bfc\u5165\u6a21\u677f.xlsx";
        String resourceFileName = "line_config_import_template.xlsx";

        File localFile = new File("import_templates", resourceFileName);
        Resource resource;
        if (localFile.exists()) {
            resource = new org.springframework.core.io.FileSystemResource(localFile);
        } else {
            resource = new ClassPathResource("import_templates/" + resourceFileName);
        }
        if (!resource.exists()) {
            resource = new ByteArrayResource(buildLineTemplateBytes());
        }

        String encoded = URLEncoder.encode(displayFileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private byte[] buildLineTemplateBytes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Data");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Line Code*");
            header.createCell(1).setCellValue("Line Name");
            header.createCell(2).setCellValue("Working Days Per Week");
            header.createCell(3).setCellValue("Shifts Per Day");
            header.createCell(4).setCellValue("Hours Per Shift");
            header.createCell(5).setCellValue("Is Active");

            Row sample1 = sheet.createRow(1);
            sample1.createCell(0).setCellValue("ASSY2001");
            sample1.createCell(1).setCellValue("ASSY浜х嚎1");
            sample1.createCell(2).setCellValue(5);
            sample1.createCell(3).setCellValue(2);
            sample1.createCell(4).setCellValue(8);
            sample1.createCell(5).setCellValue("true");

            Row sample2 = sheet.createRow(2);
            sample2.createCell(0).setCellValue("DIP2001");
            sample2.createCell(1).setCellValue("DIP浜х嚎1");
            sample2.createCell(2).setCellValue(5);
            sample2.createCell(3).setCellValue(2);
            sample2.createCell(4).setCellValue(8);
            sample2.createCell(5).setCellValue("true");

            for (int i = 0; i <= 5; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}

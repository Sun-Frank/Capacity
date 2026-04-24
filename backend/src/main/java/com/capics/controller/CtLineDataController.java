package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.CtLineDataDto;
import com.capics.dto.CtLineImportTaskDto;
import com.capics.service.CtLineDataService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ct-lines")
public class CtLineDataController {

    private final CtLineDataService ctLineDataService;

    public CtLineDataController(CtLineDataService ctLineDataService) {
        this.ctLineDataService = ctLineDataService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAll() {
        return ResponseEntity.ok(ApiResponse.success(ctLineDataService.getCtLinePageData()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody CtLineDataDto dto, Principal principal) {
        try {
            String createdBy = principal != null ? principal.getName() : "system";
            CtLineDataDto saved = ctLineDataService.create(dto, createdBy);
            return ResponseEntity.ok(ApiResponse.success("Create success", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Create failed: " + e.getMessage()));
        }
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse> importData(@RequestParam("file") MultipartFile file, Principal principal) {
        try {
            String createdBy = principal != null ? principal.getName() : "system";
            int count = ctLineDataService.importFromExcel(file, createdBy);
            return ResponseEntity.ok(ApiResponse.success("Import success, count=" + count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Import failed: " + e.getMessage()));
        }
    }

    @PostMapping("/import-async")
    public ResponseEntity<ApiResponse> importDataAsync(@RequestParam("file") MultipartFile file, Principal principal) {
        try {
            String createdBy = principal != null ? principal.getName() : "system";
            CtLineImportTaskDto task = ctLineDataService.startImportTask(file, createdBy);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("taskId", task.getTaskId());
            data.put("status", task.getStatus());
            return ResponseEntity.ok(ApiResponse.success("Import task created", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Create import task failed: " + e.getMessage()));
        }
    }

    @GetMapping("/import-tasks/{taskId}")
    public ResponseEntity<ApiResponse> getImportTask(@PathVariable String taskId) {
        try {
            CtLineImportTaskDto task = ctLineDataService.getImportTask(taskId);
            return ResponseEntity.ok(ApiResponse.success(task));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Query import task failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateById(@PathVariable Long id, @RequestBody CtLineDataDto dto, Principal principal) {
        try {
            String updatedBy = principal != null ? principal.getName() : "system";
            CtLineDataDto saved = ctLineDataService.updateById(id, dto, updatedBy);
            return ResponseEntity.ok(ApiResponse.success("Save success", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Save failed: " + e.getMessage()));
        }
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() throws Exception {
        byte[] bytes = ctLineDataService.buildTemplateFile();
        String fileName = "ct-line-template.xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .body(resource);
    }
}

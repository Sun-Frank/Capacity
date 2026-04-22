package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.CtLineDataDto;
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

    @PostMapping("/import")
    public ResponseEntity<ApiResponse> importData(@RequestParam("file") MultipartFile file, Principal principal) {
        try {
            String createdBy = principal != null ? principal.getName() : "system";
            int count = ctLineDataService.importFromExcel(file, createdBy);
            return ResponseEntity.ok(ApiResponse.success("导入成功，共 " + count + " 条"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("导入失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateById(@PathVariable Long id, @RequestBody CtLineDataDto dto, Principal principal) {
        try {
            String updatedBy = principal != null ? principal.getName() : "system";
            CtLineDataDto saved = ctLineDataService.updateById(id, dto, updatedBy);
            return ResponseEntity.ok(ApiResponse.success("保存成功", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("保存失败: " + e.getMessage()));
        }
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() throws Exception {
        byte[] bytes = ctLineDataService.buildTemplateFile();
        String fileName = "产线-产品导入模板.xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .body(resource);
    }
}

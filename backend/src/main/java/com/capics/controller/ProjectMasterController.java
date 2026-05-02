package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.ProjectMasterDto;
import com.capics.service.ProjectMasterService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

@RestController
@RequestMapping("/api/project-master")
public class ProjectMasterController {

    private final ProjectMasterService service;

    public ProjectMasterController(ProjectMasterService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> list(@RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(service.findAll(keyword)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody ProjectMasterDto dto, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Created", service.save(dto, operator(principal))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id, @RequestBody ProjectMasterDto dto, Principal principal) {
        dto.setId(id);
        return ResponseEntity.ok(ApiResponse.success("Updated", service.save(dto, operator(principal))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse> importData(@RequestParam("file") MultipartFile file, Principal principal) {
        try {
            int count = service.importFromExcel(file, operator(principal));
            return ResponseEntity.ok(ApiResponse.success("Imported " + count + " records"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Import project master failed: " + e.getMessage()));
        }
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> template() throws Exception {
        ByteArrayResource resource = new ByteArrayResource(service.buildTemplate());
        String fileName = URLEncoder.encode("项目主数据导入模板.xlsx", StandardCharsets.UTF_8.toString()).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .body(resource);
    }

    private String operator(Principal principal) {
        return principal == null ? "system" : principal.getName();
    }
}
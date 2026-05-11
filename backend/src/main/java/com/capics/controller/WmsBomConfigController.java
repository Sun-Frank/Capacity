package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.WmsBomConfigDto;
import com.capics.service.WmsBomConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/system/wms-bom-config")
public class WmsBomConfigController {
    private final WmsBomConfigService service;

    public WmsBomConfigController(WmsBomConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getConfig() {
        return ResponseEntity.ok(ApiResponse.success(service.getUiConfig()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse> saveConfig(@RequestBody WmsBomConfigDto dto, Principal principal) {
        String updatedBy = principal == null ? "system" : principal.getName();
        return ResponseEntity.ok(ApiResponse.success("Saved", service.save(dto, updatedBy)));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse> test(@RequestBody WmsBomConfigDto dto) {
        return ResponseEntity.ok(ApiResponse.success(service.testConnection(dto)));
    }
}

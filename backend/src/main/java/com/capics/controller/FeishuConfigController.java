package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.FeishuConfigDto;
import com.capics.dto.FeishuMessageRequest;
import com.capics.service.FeishuConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/system/feishu-config")
public class FeishuConfigController {

    private final FeishuConfigService service;

    public FeishuConfigController(FeishuConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getConfig() {
        return ResponseEntity.ok(ApiResponse.success(service.getUiConfig()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse> saveConfig(@RequestBody FeishuConfigDto dto, Principal principal) {
        String updatedBy = principal == null ? "system" : principal.getName();
        return ResponseEntity.ok(ApiResponse.success("Saved", service.save(dto, updatedBy)));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse> test(@RequestBody FeishuConfigDto dto) {
        return ResponseEntity.ok(ApiResponse.success(service.testConnection(dto)));
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse> send(@RequestBody FeishuMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Sent", service.sendMessage(request)));
    }
}

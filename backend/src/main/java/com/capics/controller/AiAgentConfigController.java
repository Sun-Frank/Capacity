package com.capics.controller;

import com.capics.dto.AiAgentConfigDto;
import com.capics.dto.ApiResponse;
import com.capics.service.AiAgentConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/system/ai-config")
public class AiAgentConfigController {

    private final AiAgentConfigService aiAgentConfigService;

    public AiAgentConfigController(AiAgentConfigService aiAgentConfigService) {
        this.aiAgentConfigService = aiAgentConfigService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getConfig() {
        return ResponseEntity.ok(ApiResponse.success(aiAgentConfigService.getUiConfig()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse> saveConfig(@RequestBody AiAgentConfigDto dto, Principal principal) {
        String updatedBy = principal != null ? principal.getName() : "system";
        AiAgentConfigDto saved = aiAgentConfigService.save(dto, updatedBy);
        return ResponseEntity.ok(ApiResponse.success("Saved", saved));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse> testConfig(@RequestBody AiAgentConfigDto dto) {
        return ResponseEntity.ok(ApiResponse.success(aiAgentConfigService.testConnection(dto)));
    }
}

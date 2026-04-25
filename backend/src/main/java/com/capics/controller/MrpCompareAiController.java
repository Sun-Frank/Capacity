package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.service.MrpCompareAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/mrp-compare")
public class MrpCompareAiController {

    private final MrpCompareAiService mrpCompareAiService;

    public MrpCompareAiController(MrpCompareAiService mrpCompareAiService) {
        this.mrpCompareAiService = mrpCompareAiService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse> analyze(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> result = mrpCompareAiService.analyze(payload);
            return ResponseEntity.ok(ApiResponse.success("Analyzed", result));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error("AI分析失败: " + ex.getMessage()));
        }
    }
}

package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.service.CapacityAssessmentService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/capacity-assessment")
public class CapacityAssessmentController {

    private final CapacityAssessmentService capacityAssessmentService;

    public CapacityAssessmentController(CapacityAssessmentService capacityAssessmentService) {
        this.capacityAssessmentService = capacityAssessmentService;
    }

    @GetMapping
    public ApiResponse getCapacityAssessment(
            @RequestParam String createdBy,
            @RequestParam String fileName,
            @RequestParam String version) {
        try {
            Map<String, Object> result = capacityAssessmentService.getCapacityAssessment(createdBy, fileName, version);
            return ApiResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取产能评估数据失败: " + e.getMessage());
        }
    }
}

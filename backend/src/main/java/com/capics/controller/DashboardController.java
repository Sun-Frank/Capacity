package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.service.DashboardService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/loading")
    public ApiResponse getLoadingMatrix(
            @RequestParam(defaultValue = "static") String type,
            @RequestParam(defaultValue = "week") String dimension,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String version) {
        try {
            Map<String, Object> result = dashboardService.getLoadingMatrix(type, dimension, createdBy, fileName, version);
            return ApiResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取产线负载数据失败: " + e.getMessage());
        }
    }
}

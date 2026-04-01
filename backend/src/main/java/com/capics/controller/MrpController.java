package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.MrpPlanDto;
import com.capics.service.MrpPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mrp")
public class MrpController {

    private final MrpPlanService mrpPlanService;

    public MrpController(MrpPlanService mrpPlanService) {
        this.mrpPlanService = mrpPlanService;
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse> getAllPlans() {
        List<MrpPlanDto> plans = mrpPlanService.findAll();
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @GetMapping("/plans/{id}")
    public ResponseEntity<ApiResponse> getPlanById(@PathVariable Long id) {
        MrpPlanDto plan = mrpPlanService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @GetMapping("/plans/version/{version}")
    public ResponseEntity<ApiResponse> getPlansByVersion(@PathVariable String version) {
        List<MrpPlanDto> plans = mrpPlanService.findByVersion(version);
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @GetMapping("/versions")
    public ResponseEntity<ApiResponse> getAllVersions() {
        List<String> versions = mrpPlanService.getAllVersions();
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    // 获取最新导入的MRP文件信息
    @GetMapping("/latest-file")
    public ResponseEntity<ApiResponse> getLatestMrpFile() {
        Map<String, String> fileInfo = mrpPlanService.getLatestMrpFileInfo();
        return ResponseEntity.ok(ApiResponse.success(fileInfo));
    }

    @PostMapping("/plans")
    public ResponseEntity<ApiResponse> createPlan(@RequestBody MrpPlanDto dto) {
        MrpPlanDto saved = mrpPlanService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Created", saved));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<ApiResponse> updatePlan(@PathVariable Long id, @RequestBody MrpPlanDto dto) {
        dto.setId(id);
        MrpPlanDto updated = mrpPlanService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Updated", updated));
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<ApiResponse> deletePlan(@PathVariable Long id) {
        mrpPlanService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }

    @PostMapping("/plans/import")
    public ResponseEntity<ApiResponse> importPlans(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName,
            @RequestParam("createdBy") String createdBy) throws IOException {
        int count = mrpPlanService.importFromExcel(file, fileName, createdBy);
        return ResponseEntity.ok(ApiResponse.success("Imported " + count + " records"));
    }

    // Filter endpoints for three-level cascade filtering
    @GetMapping("/filters/created-bys")
    public ResponseEntity<ApiResponse> getAllCreatedBys() {
        List<String> createdBys = mrpPlanService.getAllCreatedBys();
        return ResponseEntity.ok(ApiResponse.success(createdBys));
    }

    @GetMapping("/filters/{createdBy}/files")
    public ResponseEntity<ApiResponse> getFileNamesByCreatedBy(@PathVariable String createdBy) {
        List<String> fileNames = mrpPlanService.getFileNamesByCreatedBy(createdBy);
        return ResponseEntity.ok(ApiResponse.success(fileNames));
    }

    @GetMapping("/filters/{createdBy}/{fileName}/versions")
    public ResponseEntity<ApiResponse> getVersionsByCreatedByAndFileName(
            @PathVariable String createdBy, @PathVariable String fileName) {
        List<String> versions = mrpPlanService.getVersionsByCreatedByAndFileName(createdBy, fileName);
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    @GetMapping("/plans/filtered")
    public ResponseEntity<ApiResponse> getPlansFiltered(
            @RequestParam String createdBy,
            @RequestParam String fileName,
            @RequestParam String version) {
        List<MrpPlanDto> plans = mrpPlanService.findByCreatedByAndFileNameAndVersion(createdBy, fileName, version);
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @GetMapping("/plans/by-file")
    public ResponseEntity<ApiResponse> getPlansByFile(
            @RequestParam String createdBy,
            @RequestParam String fileName) {
        List<MrpPlanDto> plans = mrpPlanService.findByCreatedByAndFileName(createdBy, fileName);
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @GetMapping("/reports/weekly")
    public ResponseEntity<ApiResponse> getWeeklyReport(@RequestParam String version) {
        List<Map<String, Object>> report = mrpPlanService.getWeeklyReport(version);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // 周报表 - 按导入人+文件名，多版本对比
    @GetMapping("/reports/weekly/by-file")
    public ResponseEntity<ApiResponse> getWeeklyReportByFile(
            @RequestParam String createdBy,
            @RequestParam String fileName) {
        List<Map<String, Object>> report = mrpPlanService.getWeeklyReportByCreatedByAndFileName(createdBy, fileName);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // 月报表 - 按导入人+文件名，多版本对比
    @GetMapping("/reports/monthly/by-file")
    public ResponseEntity<ApiResponse> getMonthlyReportByFile(
            @RequestParam String createdBy,
            @RequestParam String fileName) {
        List<Map<String, Object>> report = mrpPlanService.getMonthlyReportByCreatedByAndFileName(createdBy, fileName);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // 单版本周需求汇总 - 按导入人+文件名+版本
    @GetMapping("/reports/weekly/single")
    public ResponseEntity<ApiResponse> getWeeklyDemandSingle(
            @RequestParam String createdBy,
            @RequestParam String fileName,
            @RequestParam String version) {
        Map<String, Object> report = mrpPlanService.getWeeklyDemandByVersion(createdBy, fileName, version);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // 单版本月需求汇总 - 按导入人+文件名+版本
    @GetMapping("/reports/monthly/single")
    public ResponseEntity<ApiResponse> getMonthlyDemandSingle(
            @RequestParam String createdBy,
            @RequestParam String fileName,
            @RequestParam String version) {
        Map<String, Object> report = mrpPlanService.getMonthlyDemandByVersion(createdBy, fileName, version);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/reports/monthly")
    public ResponseEntity<ApiResponse> getMonthlyReport(@RequestParam String version) {
        List<Map<String, Object>> report = mrpPlanService.getMonthlyReport(version);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}

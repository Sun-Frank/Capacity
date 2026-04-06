package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.service.SimulationSnapshotService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simulation-snapshots")
public class SimulationSnapshotController {

    private final SimulationSnapshotService service;

    public SimulationSnapshotController(SimulationSnapshotService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse saveSnapshot(@RequestBody Map<String, Object> body) {
        try {
            String createdBy = (String) body.get("createdBy");
            String fileName = (String) body.get("fileName");
            String version = (String) body.get("version");
            String snapshotName = (String) body.get("snapshotName");
            String source = (String) body.get("source");
            String dimension = (String) body.get("dimension");
            Object linesData = body.get("linesData");
            List<String> dates = (List<String>) body.get("dates");
            Map<String, String> dateLabels = (Map<String, String>) body.get("dateLabels");

            service.saveSnapshot(createdBy, fileName, version, snapshotName, source, dimension, linesData, dates, dateLabels);
            return ApiResponse.success("快照保存成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("保存快照失败: " + e.getMessage());
        }
    }

    @GetMapping("/names")
    public ApiResponse getSnapshotNames(
            @RequestParam String createdBy,
            @RequestParam String fileName,
            @RequestParam String version,
            @RequestParam(defaultValue = "dynamic") String source,
            @RequestParam(defaultValue = "week") String dimension) {
        try {
            List<String> names = service.getSnapshotNames(createdBy, fileName, version, source, dimension);
            return ApiResponse.success(names);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取快照列表失败: " + e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse getSnapshot(
            @RequestParam String createdBy,
            @RequestParam String fileName,
            @RequestParam String version,
            @RequestParam String snapshotName,
            @RequestParam(defaultValue = "dynamic") String source,
            @RequestParam(defaultValue = "week") String dimension) {
        try {
            Map<String, Object> snapshot = service.getSnapshot(createdBy, fileName, version, snapshotName, source, dimension);
            if (snapshot == null) {
                return ApiResponse.error("快照不存在");
            }
            return ApiResponse.success(snapshot);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取快照失败: " + e.getMessage());
        }
    }
}

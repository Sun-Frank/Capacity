package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.ManpowerPlanDto;
import com.capics.service.ManpowerPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fusion/manpower-plans")
public class ManpowerPlanController {

    private final ManpowerPlanService manpowerPlanService;

    public ManpowerPlanController(ManpowerPlanService manpowerPlanService) {
        this.manpowerPlanService = manpowerPlanService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> list(@RequestParam(required = false) String lineClass) {
        return ResponseEntity.ok(ApiResponse.success(manpowerPlanService.findAll(lineClass)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody ManpowerPlanDto dto) {
        return ResponseEntity.ok(ApiResponse.success(manpowerPlanService.save(dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        manpowerPlanService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }
}

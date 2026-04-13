package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.LineProfileDto;
import com.capics.service.LineProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fusion/line-profiles")
public class LineProfileController {

    private final LineProfileService lineProfileService;

    public LineProfileController(LineProfileService lineProfileService) {
        this.lineProfileService = lineProfileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> list() {
        return ResponseEntity.ok(ApiResponse.success(lineProfileService.findAll()));
    }

    @PutMapping("/{lineCode}")
    public ResponseEntity<ApiResponse> upsert(@PathVariable String lineCode, @RequestBody LineProfileDto dto) {
        dto.setLineCode(lineCode);
        return ResponseEntity.ok(ApiResponse.success(lineProfileService.upsert(dto)));
    }
}

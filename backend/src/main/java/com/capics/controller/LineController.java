package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.LineConfigDto;
import com.capics.service.LineConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lines")
public class LineController {

    private final LineConfigService lineConfigService;

    public LineController(LineConfigService lineConfigService) {
        this.lineConfigService = lineConfigService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAll() {
        List<LineConfigDto> lines = lineConfigService.findAll();
        return ResponseEntity.ok(ApiResponse.success(lines));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse> getActive() {
        List<LineConfigDto> lines = lineConfigService.findActive();
        return ResponseEntity.ok(ApiResponse.success(lines));
    }

    @GetMapping("/{lineCode}")
    public ResponseEntity<ApiResponse> getById(@PathVariable String lineCode) {
        LineConfigDto line = lineConfigService.findById(lineCode);
        return ResponseEntity.ok(ApiResponse.success(line));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody LineConfigDto dto) {
        LineConfigDto saved = lineConfigService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Created", saved));
    }

    @PutMapping("/{lineCode}")
    public ResponseEntity<ApiResponse> update(@PathVariable String lineCode, @RequestBody LineConfigDto dto) {
        dto.setLineCode(lineCode);
        LineConfigDto updated = lineConfigService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Updated", updated));
    }

    @DeleteMapping("/{lineCode}")
    public ResponseEntity<ApiResponse> delete(@PathVariable String lineCode) {
        lineConfigService.delete(lineCode);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }
}

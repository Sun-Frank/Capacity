package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.LineRealtimeDto;
import com.capics.service.LineRealtimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/line-realtime")
public class LineRealtimeController {

    private final LineRealtimeService lineRealtimeService;

    public LineRealtimeController(LineRealtimeService lineRealtimeService) {
        this.lineRealtimeService = lineRealtimeService;
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse> getList(
            @RequestParam(required = false) String lineCode,
            @RequestParam(required = false) String version) {

        List<LineRealtimeDto> results;
        if (lineCode != null && version != null) {
            results = lineRealtimeService.findByLineCodeAndVersion(lineCode, version);
        } else if (lineCode != null) {
            results = lineRealtimeService.findByLineCode(lineCode);
        } else if (version != null) {
            results = lineRealtimeService.findByVersion(version);
        } else {
            results = lineRealtimeService.findAll();
        }

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/{lineCode}")
    public ResponseEntity<ApiResponse> getByLineCode(@PathVariable String lineCode) {
        List<LineRealtimeDto> results = lineRealtimeService.findByLineCode(lineCode);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse> calculate(@RequestParam String version) {
        int count = lineRealtimeService.calculate(version);
        return ResponseEntity.ok(ApiResponse.success("Calculated " + count + " records"));
    }
}

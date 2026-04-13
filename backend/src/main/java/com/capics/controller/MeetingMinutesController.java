package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.MeetingMinutesDto;
import com.capics.service.MeetingMinutesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fusion/meeting-minutes")
public class MeetingMinutesController {

    private final MeetingMinutesService meetingMinutesService;

    public MeetingMinutesController(MeetingMinutesService meetingMinutesService) {
        this.meetingMinutesService = meetingMinutesService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> list(@RequestParam(required = false) String mpsVersion) {
        return ResponseEntity.ok(ApiResponse.success(meetingMinutesService.findAll(mpsVersion)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody MeetingMinutesDto dto) {
        return ResponseEntity.ok(ApiResponse.success(meetingMinutesService.save(dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        meetingMinutesService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }
}

package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.NotebookNoteDto;
import com.capics.service.NotebookNoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/notebook")
public class NotebookNoteController {

    private final NotebookNoteService service;

    public NotebookNoteController(NotebookNoteService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> list() {
        return ResponseEntity.ok(ApiResponse.success(service.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody NotebookNoteDto dto, Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Saved", service.save(dto, operator(principal))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id, @RequestBody NotebookNoteDto dto, Principal principal) {
        dto.setId(id);
        return ResponseEntity.ok(ApiResponse.success("Saved", service.save(dto, operator(principal))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }

    private String operator(Principal principal) {
        return principal == null ? "system" : principal.getName();
    }
}

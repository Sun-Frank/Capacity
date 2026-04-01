package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.SysUserDto;
import com.capics.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAll() {
        List<SysUserDto> users = userService.findAll();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getById(@PathVariable Long id) {
        SysUserDto user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<ApiResponse> getByUsername(@PathVariable String username) {
        SysUserDto user = userService.findByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody SysUserDto dto) {
        if (userService.existsByUsername(dto.getUsername())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Username already exists"));
        }
        SysUserDto saved = userService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Created", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id, @RequestBody SysUserDto dto) {
        dto.setId(id);
        SysUserDto updated = userService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse> changePassword(@PathVariable Long id, @RequestBody SysUserDto dto) {
        dto.setId(id);
        SysUserDto updated = userService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Password changed", updated));
    }
}

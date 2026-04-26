package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.LoginRequest;
import com.capics.dto.LoginResponse;
import com.capics.entity.SysUser;
import com.capics.repository.SysUserRepository;
import com.capics.service.AuthService;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final SysUserRepository userRepository;

    public AuthController(AuthService authService, PasswordEncoder passwordEncoder, SysUserRepository userRepository) {
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestParam String username, @RequestParam String password) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    user.setPassword(passwordEncoder.encode(password));
                    userRepository.save(user);
                    return ResponseEntity.ok(ApiResponse.success("Password reset successful"));
                })
                .orElse(ResponseEntity.badRequest().body(ApiResponse.error("User not found")));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse> getCurrentUser() {
        try {
            SysUser user = authService.getCurrentUser();
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

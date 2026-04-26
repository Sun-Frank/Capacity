package com.capics.service;

import com.capics.config.JwtUtil;
import com.capics.dto.LoginRequest;
import com.capics.dto.LoginResponse;
import com.capics.entity.SysUser;
import com.capics.repository.SysUserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SysUserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final LoginThrottleService loginThrottleService;

    public AuthService(AuthenticationManager authenticationManager,
                       SysUserRepository userRepository,
                       JwtUtil jwtUtil,
                       UserDetailsService userDetailsService,
                       LoginThrottleService loginThrottleService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.loginThrottleService = loginThrottleService;
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        if (loginThrottleService.isBlocked(username)) {
            long retryAfter = loginThrottleService.retryAfterSeconds(username);
            throw new LockedException("Too many failed login attempts. Try again in " + retryAfter + " seconds.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );
        } catch (AuthenticationException ex) {
            loginThrottleService.onFailure(username);
            throw ex;
        } catch (Exception ex) {
            loginThrottleService.onFailure(username);
            throw new AuthenticationServiceException("Authentication service unavailable", ex);
        }

        loginThrottleService.onSuccess(username);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String token = jwtUtil.generateToken(userDetails);

        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LoginResponse response = new LoginResponse(token, user.getUsername(), user.getRealName(), user.getId());
        List<String> roleCodes = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(v -> v != null && !v.trim().isEmpty())
                .map(v -> v.startsWith("ROLE_") ? v.substring(5) : v)
                .distinct()
                .collect(Collectors.toList());
        response.setRoleCodes(roleCodes);
        return response;
    }

    public SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Not authenticated");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}

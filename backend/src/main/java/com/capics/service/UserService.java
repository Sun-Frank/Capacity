package com.capics.service;

import com.capics.dto.SysUserDto;
import com.capics.entity.SysRole;
import com.capics.entity.SysUser;
import com.capics.entity.SysUserRole;
import com.capics.repository.SysRoleRepository;
import com.capics.repository.SysUserRoleRepository;
import com.capics.repository.SysUserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_PLAN = "PLAN";
    private static final String ROLE_MASTERDATA = "MASTERDATA";

    private final SysUserRepository repository;
    private final SysRoleRepository roleRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(SysUserRepository repository,
                       SysRoleRepository roleRepository,
                       SysUserRoleRepository userRoleRepository,
                       PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<SysUserDto> findAll() {
        List<SysUser> users = repository.findAll(Sort.by(Sort.Direction.ASC, "username"));
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public SysUserDto findById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public SysUserDto findByUsername(String username) {
        return repository.findByUsername(username)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public SysUserDto save(SysUserDto dto) {
        SysUser entity;
        String requestedRoleCode = normalizeRoleCode(dto.getRoleCode());

        if (dto.getId() != null && repository.existsById(dto.getId())) {
            entity = repository.findById(dto.getId()).get();
            if (dto.getRealName() != null) {
                entity.setRealName(dto.getRealName());
            }
            if (dto.getEmail() != null) {
                entity.setEmail(dto.getEmail());
            }
            if (dto.getEnabled() != null) {
                entity.setEnabled(dto.getEnabled());
            }
            if (dto.getUpdatedBy() != null) {
                entity.setUpdatedBy(dto.getUpdatedBy());
            }
            if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
                entity.setPassword(passwordEncoder.encode(dto.getPassword()));
            }
            if (requestedRoleCode == null) {
                requestedRoleCode = resolveRoleCodeForUser(entity.getId());
            }
        } else {
            entity = toEntity(dto);
            entity.setPassword(passwordEncoder.encode(dto.getPassword()));
            entity.setCreatedBy(dto.getUpdatedBy());
            entity.setUpdatedBy(dto.getUpdatedBy());
            if (requestedRoleCode == null) {
                requestedRoleCode = ROLE_PLAN;
            }
        }

        entity = repository.save(entity);
        bindUserRole(entity.getId(), requestedRoleCode);
        return toDto(entity);
    }

    @Transactional
    public void delete(Long id) {
        userRoleRepository.deleteByUserId(id);
        repository.deleteById(id);
    }

    public boolean existsByUsername(String username) {
        return repository.findByUsername(username).isPresent();
    }

    private SysUserDto toDto(SysUser entity) {
        SysUserDto dto = new SysUserDto();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setRealName(entity.getRealName());
        dto.setEmail(entity.getEmail());
        dto.setEnabled(entity.getEnabled());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
        dto.setRoleCode(resolveRoleCodeForUser(entity.getId()));
        return dto;
    }

    private SysUser toEntity(SysUserDto dto) {
        SysUser entity = new SysUser();
        entity.setUsername(dto.getUsername());
        entity.setRealName(dto.getRealName());
        entity.setEmail(dto.getEmail());
        entity.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        return entity;
    }

    private void bindUserRole(Long userId, String roleCode) {
        String normalized = normalizeRoleCode(roleCode);
        if (userId == null || normalized == null) {
            return;
        }

        SysRole role = roleRepository.findByRoleCode(normalized)
                .orElseThrow(() -> new RuntimeException("Role not found: " + normalized));

        userRoleRepository.deleteByUserId(userId);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleRepository.save(userRole);
    }

    private String resolveRoleCodeForUser(Long userId) {
        if (userId == null) {
            return null;
        }

        List<SysUserRole> links = userRoleRepository.findByUserId(userId);
        if (links.isEmpty()) {
            return null;
        }

        Set<Long> roleIds = links.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        Map<Long, SysRole> roleMap = roleRepository.findAllById(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, Function.identity()));

        for (SysUserRole link : links) {
            SysRole role = roleMap.get(link.getRoleId());
            if (role != null && role.getRoleCode() != null && !role.getRoleCode().trim().isEmpty()) {
                return role.getRoleCode().trim().toUpperCase(Locale.ROOT);
            }
        }
        return null;
    }

    private String normalizeRoleCode(String roleCode) {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            return null;
        }
        String normalized = roleCode.trim().toUpperCase(Locale.ROOT);
        if (!ROLE_ADMIN.equals(normalized) && !ROLE_PLAN.equals(normalized) && !ROLE_MASTERDATA.equals(normalized)) {
            throw new RuntimeException("Unsupported role: " + roleCode);
        }
        return normalized;
    }
}

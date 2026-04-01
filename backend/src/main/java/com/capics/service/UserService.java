package com.capics.service;

import com.capics.dto.SysUserDto;
import com.capics.entity.SysUser;
import com.capics.repository.SysUserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final SysUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(SysUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<SysUserDto> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "username")).stream()
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

    public SysUserDto save(SysUserDto dto) {
        SysUser entity;
        if (dto.getId() != null && repository.existsById(dto.getId())) {
            entity = repository.findById(dto.getId()).get();
            entity.setRealName(dto.getRealName());
            entity.setEmail(dto.getEmail());
            entity.setEnabled(dto.getEnabled());
            entity.setUpdatedBy(dto.getUpdatedBy());
            if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
                entity.setPassword(passwordEncoder.encode(dto.getPassword()));
            }
        } else {
            entity = toEntity(dto);
            entity.setPassword(passwordEncoder.encode(dto.getPassword()));
            entity.setCreatedBy(dto.getUpdatedBy());
            entity.setUpdatedBy(dto.getUpdatedBy());
        }
        entity = repository.save(entity);
        return toDto(entity);
    }

    public void delete(Long id) {
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
}

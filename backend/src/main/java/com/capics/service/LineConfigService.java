package com.capics.service;

import com.capics.dto.LineConfigDto;
import com.capics.entity.LineConfig;
import com.capics.repository.LineConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LineConfigService {

    private final LineConfigRepository repository;

    public LineConfigService(LineConfigRepository repository) {
        this.repository = repository;
    }

    public List<LineConfigDto> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "lineCode")).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<LineConfigDto> findActive() {
        return repository.findByIsActiveTrue().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public LineConfigDto findById(String lineCode) {
        return repository.findById(lineCode)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Line config not found"));
    }

    public LineConfigDto save(LineConfigDto dto) {
        LineConfig entity;
        if (repository.existsById(dto.getLineCode())) {
            entity = repository.findById(dto.getLineCode()).get();
            entity.setWorkingDaysPerWeek(dto.getWorkingDaysPerWeek());
            entity.setShiftsPerDay(dto.getShiftsPerDay());
            entity.setHoursPerShift(dto.getHoursPerShift());
            entity.setIsActive(dto.getIsActive());
            entity.setUpdatedBy(dto.getUpdatedBy());
        } else {
            entity = toEntity(dto);
            entity.setCreatedBy(dto.getUpdatedBy());
            entity.setUpdatedBy(dto.getUpdatedBy());
        }
        entity = repository.save(entity);
        return toDto(entity);
    }

    public boolean exists(String lineCode) {
        return repository.existsById(lineCode);
    }

    public void delete(String lineCode) {
        repository.deleteById(lineCode);
    }

    private LineConfigDto toDto(LineConfig entity) {
        LineConfigDto dto = new LineConfigDto();
        dto.setLineCode(entity.getLineCode());
        dto.setWorkingDaysPerWeek(entity.getWorkingDaysPerWeek());
        dto.setShiftsPerDay(entity.getShiftsPerDay());
        dto.setHoursPerShift(entity.getHoursPerShift());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
        return dto;
    }

    private LineConfig toEntity(LineConfigDto dto) {
        LineConfig entity = new LineConfig();
        entity.setLineCode(dto.getLineCode());
        entity.setWorkingDaysPerWeek(dto.getWorkingDaysPerWeek());
        entity.setShiftsPerDay(dto.getShiftsPerDay());
        entity.setHoursPerShift(dto.getHoursPerShift());
        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return entity;
    }
}

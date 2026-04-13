package com.capics.service;

import com.capics.dto.LineProfileDto;
import com.capics.entity.LineProfile;
import com.capics.repository.LineProfileRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LineProfileService {

    private final LineProfileRepository lineProfileRepository;

    public LineProfileService(LineProfileRepository lineProfileRepository) {
        this.lineProfileRepository = lineProfileRepository;
    }

    public List<LineProfileDto> findAll() {
        return lineProfileRepository.findAll(Sort.by(Sort.Direction.ASC, "lineCode"))
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public LineProfileDto upsert(LineProfileDto dto) {
        LineProfile entity = lineProfileRepository.findById(dto.getLineCode()).orElseGet(LineProfile::new);
        entity.setLineCode(dto.getLineCode());
        entity.setLineClass(dto.getLineClass());
        entity.setBelongTo(dto.getBelongTo());
        entity.setNote(dto.getNote());
        entity.setUpdatedBy(dto.getUpdatedBy());
        return toDto(lineProfileRepository.save(entity));
    }

    private LineProfileDto toDto(LineProfile entity) {
        LineProfileDto dto = new LineProfileDto();
        dto.setLineCode(entity.getLineCode());
        dto.setLineClass(entity.getLineClass());
        dto.setBelongTo(entity.getBelongTo());
        dto.setNote(entity.getNote());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString());
        return dto;
    }
}

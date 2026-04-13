package com.capics.service;

import com.capics.dto.MeetingMinutesDto;
import com.capics.entity.MeetingMinutes;
import com.capics.repository.MeetingMinutesRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MeetingMinutesService {

    private final MeetingMinutesRepository meetingMinutesRepository;

    public MeetingMinutesService(MeetingMinutesRepository meetingMinutesRepository) {
        this.meetingMinutesRepository = meetingMinutesRepository;
    }

    public List<MeetingMinutesDto> findAll(String mpsVersion) {
        List<MeetingMinutes> entities;
        if (mpsVersion == null || mpsVersion.isBlank()) {
            entities = meetingMinutesRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        } else {
            entities = meetingMinutesRepository.findByMpsVersionOrderByItemNoAsc(mpsVersion);
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    public MeetingMinutesDto save(MeetingMinutesDto dto) {
        MeetingMinutes entity = dto.getId() == null
                ? new MeetingMinutes()
                : meetingMinutesRepository.findById(dto.getId()).orElse(new MeetingMinutes());
        entity.setMpsVersion(dto.getMpsVersion());
        entity.setItemNo(dto.getItemNo());
        entity.setMinutes(dto.getMinutes());
        entity.setRemark(dto.getRemark());
        entity.setUpdatedBy(dto.getUpdatedBy());
        return toDto(meetingMinutesRepository.save(entity));
    }

    public void delete(Long id) {
        meetingMinutesRepository.deleteById(id);
    }

    private MeetingMinutesDto toDto(MeetingMinutes entity) {
        MeetingMinutesDto dto = new MeetingMinutesDto();
        dto.setId(entity.getId());
        dto.setMpsVersion(entity.getMpsVersion());
        dto.setItemNo(entity.getItemNo());
        dto.setMinutes(entity.getMinutes());
        dto.setRemark(entity.getRemark());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString());
        return dto;
    }
}

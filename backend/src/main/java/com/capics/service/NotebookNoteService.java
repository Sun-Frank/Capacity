package com.capics.service;

import com.capics.dto.NotebookNoteDto;
import com.capics.entity.NotebookNote;
import com.capics.repository.NotebookNoteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotebookNoteService {

    private final NotebookNoteRepository repository;

    public NotebookNoteService(NotebookNoteRepository repository) {
        this.repository = repository;
    }

    public List<NotebookNoteDto> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public NotebookNoteDto save(NotebookNoteDto dto, String operator) {
        NotebookNote entity = dto.getId() == null
                ? new NotebookNote()
                : repository.findById(dto.getId()).orElse(new NotebookNote());
        entity.setTitle(isBlank(dto.getTitle()) ? "未命名记事" : dto.getTitle().trim());
        entity.setContent(dto.getContent());
        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy(operator);
        }
        entity.setUpdatedBy(operator);
        return toDto(repository.save(entity));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private NotebookNoteDto toDto(NotebookNote entity) {
        NotebookNoteDto dto = new NotebookNoteDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString());
        return dto;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

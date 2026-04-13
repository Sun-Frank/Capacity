package com.capics.service;

import com.capics.dto.ManpowerPlanDto;
import com.capics.entity.ManpowerPlan;
import com.capics.repository.ManpowerPlanRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ManpowerPlanService {

    private final ManpowerPlanRepository manpowerPlanRepository;

    public ManpowerPlanService(ManpowerPlanRepository manpowerPlanRepository) {
        this.manpowerPlanRepository = manpowerPlanRepository;
    }

    public List<ManpowerPlanDto> findAll(String lineClass) {
        List<ManpowerPlan> entities;
        if (lineClass == null || lineClass.isBlank()) {
            entities = manpowerPlanRepository.findAll(Sort.by(Sort.Direction.DESC, "planDate"));
        } else {
            entities = manpowerPlanRepository.findByLineClassOrderByPlanDateDesc(lineClass);
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ManpowerPlanDto save(ManpowerPlanDto dto) {
        ManpowerPlan entity = dto.getId() == null
                ? new ManpowerPlan()
                : manpowerPlanRepository.findById(dto.getId()).orElse(new ManpowerPlan());

        entity.setLineClass(dto.getLineClass());
        entity.setBelongTo(dto.getBelongTo());
        entity.setManpowerFactor(dto.getManpowerFactor() == null ? BigDecimal.ONE : dto.getManpowerFactor());
        entity.setPlanDate(LocalDate.parse(dto.getPlanDate()));
        entity.setRemark(dto.getRemark());
        entity.setUpdatedBy(dto.getUpdatedBy());

        return toDto(manpowerPlanRepository.save(entity));
    }

    public void delete(Long id) {
        manpowerPlanRepository.deleteById(id);
    }

    public BigDecimal resolveFactor(String lineClass, LocalDate date) {
        if (lineClass == null || lineClass.isBlank()) {
            return BigDecimal.ONE;
        }
        Optional<ManpowerPlan> planOpt = manpowerPlanRepository
                .findTopByLineClassAndPlanDateLessThanEqualOrderByPlanDateDesc(lineClass, date);
        if (planOpt.isEmpty()) {
            return BigDecimal.ONE;
        }
        BigDecimal factor = planOpt.get().getManpowerFactor();
        if (factor == null || factor.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return factor;
    }

    private ManpowerPlanDto toDto(ManpowerPlan entity) {
        ManpowerPlanDto dto = new ManpowerPlanDto();
        dto.setId(entity.getId());
        dto.setLineClass(entity.getLineClass());
        dto.setBelongTo(entity.getBelongTo());
        dto.setManpowerFactor(entity.getManpowerFactor());
        dto.setPlanDate(entity.getPlanDate() == null ? null : entity.getPlanDate().toString());
        dto.setRemark(entity.getRemark());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString());
        return dto;
    }
}

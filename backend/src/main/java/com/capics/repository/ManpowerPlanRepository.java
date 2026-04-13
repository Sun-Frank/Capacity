package com.capics.repository;

import com.capics.entity.ManpowerPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ManpowerPlanRepository extends JpaRepository<ManpowerPlan, Long> {

    List<ManpowerPlan> findByLineClassOrderByPlanDateDesc(String lineClass);

    Optional<ManpowerPlan> findTopByLineClassAndPlanDateLessThanEqualOrderByPlanDateDesc(String lineClass, LocalDate planDate);
}

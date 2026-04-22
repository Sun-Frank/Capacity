package com.capics.repository;

import com.capics.entity.CtLineData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CtLineDataRepository extends JpaRepository<CtLineData, Long> {
    Optional<CtLineData> findTopByColDAndColBAndColCOrderByIdDesc(String colD, String colB, String colC);
    List<CtLineData> findByColDAndColCOrderByIdDesc(String colD, String colC);
}

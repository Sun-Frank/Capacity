package com.capics.repository;

import com.capics.entity.FamilyLine;
import com.capics.entity.FamilyLineId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FamilyLineRepository extends JpaRepository<FamilyLine, FamilyLineId> {
    List<FamilyLine> findAll();
    List<FamilyLine> findByFamilyCodeContainingIgnoreCase(String familyCode);
}

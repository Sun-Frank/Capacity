package com.capics.repository;

import com.capics.entity.ProductFamily;
import com.capics.entity.ProductFamilyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductFamilyRepository extends JpaRepository<ProductFamily, ProductFamilyId> {
    List<ProductFamily> findByFamilyCode(String familyCode);
    List<ProductFamily> findByLineCode(String lineCode);
    List<ProductFamily> findByFamilyCodeContainingIgnoreCase(String familyCode);
    Optional<ProductFamily> findByFamilyCodeAndLineCode(String familyCode, String lineCode);
}

package com.capics.repository;

import com.capics.entity.ProductFamily;
import com.capics.entity.ProductFamilyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductFamilyRepository extends JpaRepository<ProductFamily, ProductFamilyId> {
    List<ProductFamily> findByFamilyCode(String familyCode);
    List<ProductFamily> findByLineCode(String lineCode);
    List<ProductFamily> findByFamilyCodeContainingIgnoreCase(String familyCode);
}

package com.capics.repository;

import com.capics.entity.Product;
import com.capics.entity.ProductId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, ProductId> {
    List<Product> findByItemNumber(String itemNumber);
    List<Product> findByLineCode(String lineCode);
    List<Product> findByFamilyCode(String familyCode);
    List<Product> findByFamilyCodeAndLineCode(String familyCode, String lineCode);
    List<Product> findByItemNumberContainingIgnoreCase(String itemNumber);
}

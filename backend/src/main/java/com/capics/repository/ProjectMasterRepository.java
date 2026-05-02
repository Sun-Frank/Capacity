package com.capics.repository;

import com.capics.entity.ProjectMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectMasterRepository extends JpaRepository<ProjectMaster, Long> {
    boolean existsByProductDescriptionIgnoreCase(String productDescription);
    List<ProjectMaster> findByProductDescriptionContainingIgnoreCase(String keyword);

    @Query("select p from ProjectMaster p where lower(coalesce(p.customer, '')) like lower(concat('%', ?1, '%'))"
            + " or lower(coalesce(p.productPlatform, '')) like lower(concat('%', ?1, '%'))"
            + " or lower(coalesce(p.vehicleConfig, '')) like lower(concat('%', ?1, '%'))"
            + " or lower(coalesce(p.productDescription, '')) like lower(concat('%', ?1, '%'))"
            + " or lower(coalesce(p.bws, '')) like lower(concat('%', ?1, '%'))"
            + " or lower(coalesce(p.version, '')) like lower(concat('%', ?1, '%'))")
    List<ProjectMaster> search(String keyword);
}

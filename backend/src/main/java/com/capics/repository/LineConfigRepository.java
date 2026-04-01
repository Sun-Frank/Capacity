package com.capics.repository;

import com.capics.entity.LineConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LineConfigRepository extends JpaRepository<LineConfig, String> {
    List<LineConfig> findByIsActiveTrue();
}

package com.capics.repository;

import com.capics.entity.FeishuConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeishuConfigRepository extends JpaRepository<FeishuConfig, Integer> {
}

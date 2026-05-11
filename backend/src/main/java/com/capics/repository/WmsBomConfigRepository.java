package com.capics.repository;

import com.capics.entity.WmsBomConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WmsBomConfigRepository extends JpaRepository<WmsBomConfig, Integer> {
}

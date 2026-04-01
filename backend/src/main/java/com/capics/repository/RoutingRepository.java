package com.capics.repository;

import com.capics.entity.Routing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoutingRepository extends JpaRepository<Routing, Long> {
    Optional<Routing> findByProductNumber(String productNumber);
    List<Routing> findAllByProductNumber(String productNumber);
}

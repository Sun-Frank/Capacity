package com.capics.repository;

import com.capics.entity.RoutingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoutingItemRepository extends JpaRepository<RoutingItem, Long> {
    List<RoutingItem> findByRoutingId(Long routingId);
    List<RoutingItem> findByComponentNumberAndLineCode(String componentNumber, String lineCode);
    void deleteByRoutingId(Long routingId);
}

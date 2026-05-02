package com.capics.repository;

import com.capics.entity.RoutingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoutingItemRepository extends JpaRepository<RoutingItem, Long> {
    List<RoutingItem> findByRoutingId(Long routingId);
    List<RoutingItem> findByRoutingIdIn(List<Long> routingIds);
    List<RoutingItem> findByComponentNumberAndLineCode(String componentNumber, String lineCode);
    @Query(value = "select i.* from routing_item i join routing r on i.routing_id = r.id where i.component_number = ?1 order by r.created_at asc, r.id asc, i.id asc", nativeQuery = true)
    List<RoutingItem> findFirstMaintainedByComponentNumber(String componentNumber);
    void deleteByRoutingId(Long routingId);
}

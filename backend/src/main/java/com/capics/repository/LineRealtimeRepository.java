package com.capics.repository;

import com.capics.entity.LineRealtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LineRealtimeRepository extends JpaRepository<LineRealtime, Long> {
    List<LineRealtime> findByLineCode(String lineCode);
    List<LineRealtime> findByMrpVersion(String mrpVersion);
    List<LineRealtime> findByLineCodeAndMrpVersion(String lineCode, String mrpVersion);
    void deleteByMrpVersion(String mrpVersion);
}

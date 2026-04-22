package com.capics.repository;

import com.capics.entity.SimulationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SimulationSnapshotRepository extends JpaRepository<SimulationSnapshot, Long> {

    List<SimulationSnapshot> findByCreatedByAndFileNameAndVersion(
            String createdBy, String fileName, String version);

    List<SimulationSnapshot> findByCreatedByAndFileNameAndVersionAndSourceAndDimensionOrderByCreatedAtDesc(
            String createdBy, String fileName, String version, String source, String dimension);

    List<SimulationSnapshot> findByCreatedByAndFileNameAndVersionAndSnapshotName(
            String createdBy, String fileName, String version, String snapshotName);

    List<SimulationSnapshot> findByCreatedByAndFileNameAndVersionAndSnapshotNameAndSourceAndDimension(
            String createdBy, String fileName, String version, String snapshotName, String source, String dimension);
}

package com.capics.repository;

import com.capics.entity.MrpPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MrpPlanRepository extends JpaRepository<MrpPlan, Long> {

    List<MrpPlan> findByVersion(String version);

    List<MrpPlan> findByItemNumber(String itemNumber);

    List<MrpPlan> findByReleaseDateBetween(LocalDate startDate, LocalDate endDate);

    List<MrpPlan> findByVersionAndReleaseDateBetween(String version, LocalDate startDate, LocalDate endDate);

    @Query("SELECT DISTINCT m.version FROM MrpPlan m ORDER BY m.version DESC")
    List<String> findAllVersions();

    @Query("SELECT m FROM MrpPlan m WHERE m.version = :version ORDER BY m.itemNumber, m.releaseDate")
    List<MrpPlan> findByVersionOrderByItemNumberAndReleaseDate(String version);

    // New filter methods for three-level cascade filtering
    @Query("SELECT DISTINCT m.createdBy FROM MrpPlan m WHERE m.createdBy IS NOT NULL ORDER BY m.createdBy")
    List<String> findAllCreatedBys();

    @Query("SELECT DISTINCT m.fileName FROM MrpPlan m WHERE m.createdBy = :createdBy AND m.fileName IS NOT NULL ORDER BY m.fileName")
    List<String> findFileNamesByCreatedBy(String createdBy);

    @Query("SELECT DISTINCT m.version FROM MrpPlan m WHERE m.createdBy = :createdBy AND m.fileName = :fileName ORDER BY m.version DESC")
    List<String> findVersionsByCreatedByAndFileName(String createdBy, String fileName);

    List<MrpPlan> findByCreatedByAndFileNameAndVersion(String createdBy, String fileName, String version);

    List<MrpPlan> findByCreatedByAndFileName(String createdBy, String fileName);

    boolean existsByCreatedByAndFileName(String createdBy, String fileName);

    @Query("SELECT m FROM MrpPlan m WHERE m.createdBy = :createdBy AND m.fileName = :fileName ORDER BY m.itemNumber, m.releaseDate")
    List<MrpPlan> findByCreatedByAndFileNameOrderByItemNumberAndReleaseDate(@Param("createdBy") String createdBy, @Param("fileName") String fileName);

    @Query("SELECT m FROM MrpPlan m WHERE m.createdBy = :createdBy AND m.fileName = :fileName AND m.version = :version ORDER BY m.itemNumber, m.releaseDate")
    List<MrpPlan> findByCreatedByAndFileNameAndVersionOrderByItemNumberAndReleaseDate(
            @Param("createdBy") String createdBy,
            @Param("fileName") String fileName,
            @Param("version") String version);

    @Query("SELECT m FROM MrpPlan m WHERE m.fileName IS NOT NULL AND m.createdBy IS NOT NULL ORDER BY m.createdAt DESC")
    List<MrpPlan> findLatestFileInfo();
}

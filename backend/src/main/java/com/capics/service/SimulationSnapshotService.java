package com.capics.service;

import com.capics.entity.SimulationSnapshot;
import com.capics.repository.SimulationSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SimulationSnapshotService {

    private final SimulationSnapshotRepository repository;
    private final ObjectMapper objectMapper;

    public SimulationSnapshotService(SimulationSnapshotRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SimulationSnapshot saveSnapshot(
            String createdBy,
            String fileName,
            String version,
            String snapshotName,
            String source,
            String dimension,
            Object linesData,
            List<String> dates,
            Map<String, String> dateLabels) throws Exception {

        SimulationSnapshot snapshot = new SimulationSnapshot();
        snapshot.setCreatedBy(createdBy);
        snapshot.setFileName(fileName);
        snapshot.setVersion(version);
        snapshot.setSnapshotName(snapshotName);
        snapshot.setSource(source);
        snapshot.setDimension(dimension);
        snapshot.setLinesData(objectMapper.writeValueAsString(linesData));
        snapshot.setDates(objectMapper.writeValueAsString(dates));
        snapshot.setDateLabels(objectMapper.writeValueAsString(dateLabels));

        return repository.save(snapshot);
    }

    public List<String> getSnapshotNames(String createdBy, String fileName, String version, String source, String dimension) {
        // 查询 week 和 month 两个维度，返回所有匹配的快照名
        List<SimulationSnapshot> weekSnapshots = repository.findByCreatedByAndFileNameAndVersionAndSourceAndDimensionOrderByCreatedAtDesc(
                createdBy, fileName, version, source, "week");
        List<SimulationSnapshot> monthSnapshots = repository.findByCreatedByAndFileNameAndVersionAndSourceAndDimensionOrderByCreatedAtDesc(
                createdBy, fileName, version, source, "month");
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        // 按时间倒序添加：先加 month（最新），再加 week
        for (SimulationSnapshot s : monthSnapshots) names.add(s.getSnapshotName());
        for (SimulationSnapshot s : weekSnapshots) names.add(s.getSnapshotName());
        return new java.util.ArrayList<>(names);
    }

    public Map<String, Object> getSnapshot(String createdBy, String fileName, String version,
                                          String snapshotName, String source, String dimension) throws Exception {
        List<SimulationSnapshot> snapshots = repository.findByCreatedByAndFileNameAndVersionAndSnapshotName(
                createdBy, fileName, version, snapshotName);

        // Filter by source and dimension; if not found, try the other dimension
        String otherDimension = "week".equals(dimension) ? "month" : "week";
        SimulationSnapshot found = null;
        for (SimulationSnapshot s : snapshots) {
            if (source.equals(s.getSource()) && dimension.equals(s.getDimension())) {
                found = s;
                break;
            }
        }
        if (found == null) {
            for (SimulationSnapshot s : snapshots) {
                if (source.equals(s.getSource()) && otherDimension.equals(s.getDimension())) {
                    found = s;
                    break;
                }
            }
        }

        if (found != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("source", found.getSource());
            result.put("dimension", found.getDimension());
            result.put("snapshotName", found.getSnapshotName());
            result.put("dates", objectMapper.readValue(found.getDates(), List.class));
            result.put("dateLabels", objectMapper.readValue(found.getDateLabels(), Map.class));
            result.put("linesData", objectMapper.readValue(found.getLinesData(), Map.class));
            result.put("createdAt", found.getCreatedAt().toString());
            return result;
        }
        return null;
    }
}

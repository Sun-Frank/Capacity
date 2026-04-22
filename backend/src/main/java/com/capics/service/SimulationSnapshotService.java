package com.capics.service;

import com.capics.entity.SimulationSnapshot;
import com.capics.repository.SimulationSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        createdBy = normalizeKey(createdBy);
        fileName = normalizeKey(fileName);
        version = normalizeKey(version);
        snapshotName = normalizeKey(snapshotName);
        source = normalizeKind(source, "dynamic");
        dimension = normalizeKind(dimension, "week");

        List<SimulationSnapshot> existing = repository
                .findByCreatedByAndFileNameAndVersionAndSnapshotNameAndSourceAndDimension(
                        createdBy, fileName, version, snapshotName, source, dimension);
        if (!existing.isEmpty()) {
            repository.deleteAll(existing);
        }

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
        createdBy = normalizeKey(createdBy);
        fileName = normalizeKey(fileName);
        version = normalizeKey(version);
        source = normalizeKind(source, "dynamic");
        dimension = normalizeKind(dimension, "week");

        List<SimulationSnapshot> snapshots = repository
                .findByCreatedByAndFileNameAndVersionAndSourceAndDimensionOrderByCreatedAtDesc(
                        createdBy, fileName, version, source, dimension);
        Set<String> names = new LinkedHashSet<>();
        for (SimulationSnapshot snapshot : snapshots) {
            names.add(snapshot.getSnapshotName());
        }
        return new ArrayList<>(names);
    }

    public Map<String, Object> getSnapshot(
            String createdBy,
            String fileName,
            String version,
            String snapshotName,
            String source,
            String dimension) throws Exception {
        createdBy = normalizeKey(createdBy);
        fileName = normalizeKey(fileName);
        version = normalizeKey(version);
        snapshotName = normalizeKey(snapshotName);
        source = normalizeKind(source, "dynamic");
        dimension = normalizeKind(dimension, "week");

        List<SimulationSnapshot> snapshots = repository.findByCreatedByAndFileNameAndVersionAndSnapshotName(
                createdBy, fileName, version, snapshotName);

        String otherDimension = "week".equals(dimension) ? "month" : "week";
        SimulationSnapshot found = null;
        for (SimulationSnapshot snapshot : snapshots) {
            if (source.equals(snapshot.getSource()) && dimension.equals(snapshot.getDimension())) {
                found = snapshot;
                break;
            }
        }
        if (found == null) {
            for (SimulationSnapshot snapshot : snapshots) {
                if (source.equals(snapshot.getSource()) && otherDimension.equals(snapshot.getDimension())) {
                    found = snapshot;
                    break;
                }
            }
        }

        if (found == null) {
            return null;
        }

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

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeKind(String value, String defaultValue) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? defaultValue : normalized;
    }
}

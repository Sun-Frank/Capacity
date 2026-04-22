package com.capics.service;

import com.capics.dto.BomExpandDto;
import com.capics.dto.LineConfigDto;
import com.capics.dto.LineRealtimeDto;
import com.capics.entity.LineConfig;
import com.capics.entity.LineRealtime;
import com.capics.entity.MrpPlan;
import com.capics.entity.Product;
import com.capics.entity.ProductId;
import com.capics.repository.LineConfigRepository;
import com.capics.repository.LineRealtimeRepository;
import com.capics.repository.MrpPlanRepository;
import com.capics.repository.ProductRepository;
import com.capics.repository.RoutingItemRepository;
import com.capics.repository.RoutingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LineRealtimeService {

    private final LineRealtimeRepository realtimeRepository;
    private final LineConfigRepository lineConfigRepository;
    private final MrpPlanRepository mrpPlanRepository;
    private final ProductRepository productRepository;
    private final RoutingRepository routingRepository;
    private final RoutingItemRepository routingItemRepository;

    public LineRealtimeService(LineRealtimeRepository realtimeRepository,
                               LineConfigRepository lineConfigRepository,
                               MrpPlanRepository mrpPlanRepository,
                               ProductRepository productRepository,
                               RoutingRepository routingRepository,
                               RoutingItemRepository routingItemRepository) {
        this.realtimeRepository = realtimeRepository;
        this.lineConfigRepository = lineConfigRepository;
        this.mrpPlanRepository = mrpPlanRepository;
        this.productRepository = productRepository;
        this.routingRepository = routingRepository;
        this.routingItemRepository = routingItemRepository;
    }

    public List<LineRealtimeDto> findAll() {
        return realtimeRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<LineRealtimeDto> findByLineCode(String lineCode) {
        return realtimeRepository.findByLineCode(lineCode).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<LineRealtimeDto> findByVersion(String mrpVersion) {
        return realtimeRepository.findByMrpVersion(mrpVersion).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<LineRealtimeDto> findByLineCodeAndVersion(String lineCode, String mrpVersion) {
        return realtimeRepository.findByLineCodeAndMrpVersion(lineCode, mrpVersion).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public int calculate(String mrpVersion) {
        String versionKey = mrpVersion == null ? "" : mrpVersion.trim();
        realtimeRepository.deleteByMrpVersion(versionKey);
        realtimeRepository.flush();

        List<MrpPlan> mrpPlans = mrpPlanRepository.findByVersion(versionKey);
        List<LineConfig> lineConfigs = lineConfigRepository.findByIsActiveTrue();
        Set<String> insertedKeys = new HashSet<>();

        int count = 0;

        for (MrpPlan plan : mrpPlans) {
            var routingOpt = routingRepository.findByProductNumber(plan.getItemNumber());
            if (routingOpt.isEmpty()) continue;

            var routingItems = routingItemRepository.findByRoutingId(routingOpt.get().getId());

            for (LineConfig line : lineConfigs) {
                for (var item : routingItems) {
                    String dedupeKey = line.getLineCode() + "|" + plan.getItemNumber() + "|" + item.getComponentNumber();
                    if (!insertedKeys.add(dedupeKey)) {
                        continue;
                    }

                    LineRealtime entity = new LineRealtime();
                    entity.setLineCode(line.getLineCode());
                    entity.setItemNumber(plan.getItemNumber());
                    entity.setComponentNumber(item.getComponentNumber());
                    entity.setMrpVersion(versionKey);

                    Product product = productRepository.findById(
                            new ProductId(item.getComponentNumber(), line.getLineCode())
                    ).orElse(null);

                    if (product == null) continue;

                    entity.setCt(product.getCycleTime());
                    entity.setOee(product.getOee());
                    entity.setShiftWorkers(product.getWorkerCount());

                    BigDecimal cycleTime = product.getCycleTime();
                    BigDecimal oee = product.getOee();
                    BigDecimal hoursPerShift = line.getHoursPerShift();
                    if (cycleTime == null || cycleTime.compareTo(BigDecimal.ZERO) <= 0) continue;
                    if (oee == null || hoursPerShift == null) continue;

                    BigDecimal oeeDecimal = oee.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

                    BigDecimal shiftOutput = BigDecimal.valueOf(3600)
                            .divide(cycleTime, 4, RoundingMode.HALF_UP)
                            .multiply(oeeDecimal)
                            .multiply(hoursPerShift)
                            .setScale(2, RoundingMode.HALF_UP);

                    entity.setShiftOutput(shiftOutput);

                    Map<String, BigDecimal> weeklyDemand = calculateWeeklyDemand(
                            plan.getItemNumber(), item.getComponentNumber(), versionKey);
                    entity.setWeeklyDemand(toJson(weeklyDemand));

                    realtimeRepository.save(entity);
                    count++;
                }
            }
        }

        return count;
    }

    private Map<String, BigDecimal> calculateWeeklyDemand(String itemNumber, String componentNumber, String mrpVersion) {
        List<MrpPlan> plans = mrpPlanRepository.findByVersion(mrpVersion);
        Map<String, BigDecimal> weeklyDemand = new LinkedHashMap<>();

        for (MrpPlan plan : plans) {
            if (!plan.getItemNumber().equals(itemNumber)) continue;

            LocalDate releaseDate = plan.getReleaseDate();
            int weekOfYear = releaseDate.get(WeekFields.of(Locale.getDefault()).weekOfYear());
            String weekKey = String.format("%02d", weekOfYear);

            BigDecimal current = weeklyDemand.getOrDefault(weekKey, BigDecimal.ZERO);
            weeklyDemand.put(weekKey, current.add(plan.getQuantityScheduled()));
        }

        return weeklyDemand;
    }

    private String toJson(Map<String, BigDecimal> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":")
              .append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private LineRealtimeDto toDto(LineRealtime entity) {
        LineRealtimeDto dto = new LineRealtimeDto();
        dto.setId(entity.getId());
        dto.setLineCode(entity.getLineCode());
        dto.setItemNumber(entity.getItemNumber());
        dto.setComponentNumber(entity.getComponentNumber());
        dto.setDescription(entity.getDescription());
        dto.setShiftOutput(entity.getShiftOutput());
        dto.setShiftWorkers(entity.getShiftWorkers());
        dto.setCt(entity.getCt());
        dto.setOee(entity.getOee());
        dto.setWeeklyDemand(entity.getWeeklyDemand());
        dto.setMrpVersion(entity.getMrpVersion());
        dto.setCalculatedAt(entity.getCalculatedAt());
        return dto;
    }
}

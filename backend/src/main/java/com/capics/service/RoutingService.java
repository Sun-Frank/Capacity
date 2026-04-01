package com.capics.service;

import com.capics.dto.BomExpandDto;
import com.capics.dto.RoutingDto;
import com.capics.dto.RoutingItemDto;
import com.capics.entity.Product;
import com.capics.entity.ProductId;
import com.capics.entity.Routing;
import com.capics.entity.RoutingItem;
import com.capics.repository.ProductRepository;
import com.capics.repository.RoutingItemRepository;
import com.capics.repository.RoutingRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoutingService {

    private final RoutingRepository routingRepository;
    private final RoutingItemRepository routingItemRepository;
    private final ProductRepository productRepository;

    public RoutingService(RoutingRepository routingRepository,
                         RoutingItemRepository routingItemRepository,
                         ProductRepository productRepository) {
        this.routingRepository = routingRepository;
        this.routingItemRepository = routingItemRepository;
        this.productRepository = productRepository;
    }

    public List<RoutingDto> findAll() {
        return routingRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // 返回完整的工艺路线信息（每个RoutingItem一行）
    public List<RoutingItemDto> findAllItems() {
        List<Routing> routings = routingRepository.findAll();
        List<RoutingItemDto> result = new ArrayList<>();
        for (Routing routing : routings) {
            List<RoutingItem> items = routingItemRepository.findByRoutingId(routing.getId());
            for (RoutingItem item : items) {
                RoutingItemDto dto = toItemDto(item);
                dto.setProductNumber(routing.getProductNumber());
                dto.setRoutingDescription(routing.getDescription());
                result.add(dto);
            }
        }
        // 按成品物料号和BOM层级降序排序
        result.sort((a, b) -> {
            int cmp = a.getProductNumber().compareTo(b.getProductNumber());
            if (cmp != 0) return cmp;
            return b.getBomLevel().compareTo(a.getBomLevel());
        });
        return result;
    }

    // 按生产线分组获取所有RoutingItem数据
    public Map<String, List<RoutingItemDto>> findAllItemsGroupedByLine() {
        List<RoutingItemDto> allItems = findAllItems();
        Map<String, List<RoutingItemDto>> grouped = new LinkedHashMap<>();
        for (RoutingItemDto item : allItems) {
            String lineCode = item.getLineCode();
            if (!grouped.containsKey(lineCode)) {
                grouped.put(lineCode, new ArrayList<>());
            }
            grouped.get(lineCode).add(item);
        }
        return grouped;
    }

    // 更新组件的生产线
    @Transactional
    public RoutingItemDto updateRoutingItemLineCode(Long id, String lineCode, String updatedBy) {
        RoutingItem item = routingItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RoutingItem not found: " + id));
        item.setLineCode(lineCode);
        item = routingItemRepository.save(item);

        RoutingItemDto dto = toItemDto(item);

        // 获取成品信息
        Routing routing = routingRepository.findById(item.getRoutingId()).orElse(null);
        if (routing != null) {
            dto.setProductNumber(routing.getProductNumber());
            dto.setRoutingDescription(routing.getDescription());
        }

        return dto;
    }

    public RoutingDto findById(Long id) {
        return routingRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Routing not found"));
    }

    public RoutingDto findByProductNumber(String productNumber) {
        return routingRepository.findByProductNumber(productNumber)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Routing not found"));
    }

    public RoutingDto save(RoutingDto dto) {
        Routing entity = toEntity(dto);
        entity = routingRepository.save(entity);
        return toDto(entity);
    }

    public void delete(Long id) {
        routingRepository.deleteById(id);
    }

    // 检查导入文件中的重复成品物料号
    public List<Map<String, String>> checkDuplicates(MultipartFile file) throws IOException {
        List<Map<String, String>> duplicates = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell cell = row.getCell(0);
                if (cell == null) continue;

                String productNumber = cell.getCellType() == CellType.STRING
                    ? cell.getStringCellValue().trim()
                    : String.valueOf((long) cell.getNumericCellValue());

                if (productNumber != null && !productNumber.isEmpty()) {
                    // 使用返回列表的方法避免 NonUniqueResultException
                    List<Routing> existing = routingRepository.findAllByProductNumber(productNumber);
                    if (!existing.isEmpty()) {
                        Map<String, String> dup = new HashMap<>();
                        dup.put("productNumber", productNumber);
                        duplicates.add(dup);
                    }
                }
            }
        }

        return duplicates;
    }

    @Transactional
    public int importFromExcel(MultipartFile file, String createdBy) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int count = 0;

            Routing currentRouting = null;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String productNumber = getCellValueAsString(row.getCell(0));
                String description = getCellValueAsString(row.getCell(1));
                String componentNumber = getCellValueAsString(row.getCell(2));
                String lineCode = getCellValueAsString(row.getCell(3));
                Integer bomLevel = getCellValueAsInteger(row.getCell(4));

                // 如果有成品物料号，创建新的工艺路线
                if (productNumber != null && !productNumber.isEmpty()) {
                    Routing routing = new Routing();
                    routing.setProductNumber(productNumber);
                    routing.setDescription(description);
                    routing.setCreatedBy(createdBy);
                    currentRouting = routingRepository.save(routing);
                    count++;
                }

                // 如果有组件物料号、生产线编码、BOM层级，添加工艺路线组件
                if (currentRouting != null
                        && componentNumber != null && !componentNumber.isEmpty()
                        && lineCode != null && !lineCode.isEmpty()
                        && bomLevel != null) {
                    RoutingItem item = new RoutingItem();
                    item.setRoutingId(currentRouting.getId());
                    item.setComponentNumber(componentNumber);
                    item.setLineCode(lineCode);
                    item.setBomLevel(bomLevel);
                    item.setBomQuantity(BigDecimal.ONE);
                    routingItemRepository.save(item);
                }
            }

            return count;
        }
    }

    public List<RoutingItemDto> getByProductNumber(String productNumber) {
        Routing routing = routingRepository.findByProductNumber(productNumber)
                .orElseThrow(() -> new RuntimeException("Routing not found"));

        return routingItemRepository.findByRoutingId(routing.getId()).stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());
    }

    public BomExpandDto expandBom(String itemNumber, BigDecimal quantity) {
        Routing routing = routingRepository.findByProductNumber(itemNumber)
                .orElseThrow(() -> new RuntimeException("Routing not found for: " + itemNumber));

        List<RoutingItem> items = routingItemRepository.findByRoutingId(routing.getId());

        BomExpandDto result = new BomExpandDto();
        result.setItemNumber(itemNumber);
        result.setComponents(new ArrayList<>());

        for (RoutingItem item : items) {
            BomExpandDto.BomComponent component = new BomExpandDto.BomComponent();
            component.setComponentNumber(item.getComponentNumber());
            component.setLineCode(item.getLineCode());
            component.setQuantity(quantity.multiply(item.getBomQuantity()));

            Product product = productRepository.findById(
                    new ProductId(item.getComponentNumber(), item.getLineCode())
            ).orElse(null);

            if (product != null) {
                component.setCt(product.getCycleTime());
                component.setOee(product.getOee());
                component.setWorkers(product.getWorkerCount());
            }

            result.getComponents().add(component);
        }

        return result;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        return null;
    }

    private RoutingDto toDto(Routing entity) {
        RoutingDto dto = new RoutingDto();
        dto.setId(entity.getId());
        dto.setProductNumber(entity.getProductNumber());
        dto.setDescription(entity.getDescription());
        dto.setVersion(entity.getVersion());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
        return dto;
    }

    private Routing toEntity(RoutingDto dto) {
        Routing entity = new Routing();
        if (dto.getId() != null) {
            entity.setId(dto.getId());
        }
        entity.setProductNumber(dto.getProductNumber());
        entity.setDescription(dto.getDescription());
        entity.setVersion(dto.getVersion());
        return entity;
    }

    private RoutingItemDto toItemDto(RoutingItem entity) {
        RoutingItemDto dto = new RoutingItemDto();
        dto.setId(entity.getId());
        dto.setRoutingId(entity.getRoutingId());
        dto.setComponentNumber(entity.getComponentNumber());
        dto.setLineCode(entity.getLineCode());
        dto.setBomLevel(entity.getBomLevel());
        dto.setBomQuantity(entity.getBomQuantity());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        return dto;
    }
}

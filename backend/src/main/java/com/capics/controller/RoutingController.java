package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.BomExpandDto;
import com.capics.dto.RoutingDto;
import com.capics.dto.RoutingItemDto;
import com.capics.service.RoutingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/routings")
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAll() {
        List<RoutingDto> routings = routingService.findAll();
        return ResponseEntity.ok(ApiResponse.success(routings));
    }

    // 获取完整的工艺路线信息（每个组件一行）
    @GetMapping("/full")
    public ResponseEntity<ApiResponse> getAllFull() {
        List<RoutingItemDto> items = routingService.findAllItems();
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getById(@PathVariable Long id) {
        RoutingDto routing = routingService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(routing));
    }

    @GetMapping("/product/{productNumber}")
    public ResponseEntity<ApiResponse> getByProductNumber(@PathVariable String productNumber) {
        RoutingDto routing = routingService.findByProductNumber(productNumber);
        return ResponseEntity.ok(ApiResponse.success(routing));
    }

    @GetMapping("/by-product/{productNumber}")
    public ResponseEntity<ApiResponse> getItemsByProduct(@PathVariable String productNumber) {
        List<RoutingItemDto> items = routingService.getByProductNumber(productNumber);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @PostMapping("/expand")
    public ResponseEntity<ApiResponse> expandBom(@RequestBody BomExpandRequest request) {
        BomExpandDto result = routingService.expandBom(request.getItemNumber(), request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody RoutingDto dto) {
        RoutingDto saved = routingService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Created", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id, @RequestBody RoutingDto dto) {
        dto.setId(id);
        RoutingDto updated = routingService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        routingService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }

    // 检查导入文件中的重复成品物料号
    @PostMapping("/import/check")
    public ResponseEntity<ApiResponse> checkImportDuplicates(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, String>> duplicates = routingService.checkDuplicates(file);
        if (duplicates.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No duplicates found", null));
        } else {
            return ResponseEntity.ok(new ApiResponse(false, "Found " + duplicates.size() + " duplicate(s)", duplicates));
        }
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse> importRoutings(
            @RequestParam("file") MultipartFile file,
            @RequestParam("createdBy") String createdBy,
            @RequestParam(value = "overwrite", defaultValue = "false") boolean overwrite) throws IOException {
        int count = routingService.importFromExcel(file, createdBy, overwrite);
        return ResponseEntity.ok(ApiResponse.success("Imported " + count + " records"));
    }

    // 获取按生产线分组的工艺路线组件数据
    @GetMapping("/items/grouped")
    public ResponseEntity<ApiResponse> getItemsGroupedByLine() {
        Map<String, List<RoutingItemDto>> grouped = routingService.findAllItemsGroupedByLine();
        return ResponseEntity.ok(ApiResponse.success(grouped));
    }

    // 更新组件的生产线
    @PutMapping("/items/{id}/line")
    public ResponseEntity<ApiResponse> updateItemLine(
            @PathVariable Long id,
            @RequestBody UpdateLineRequest request) {
        RoutingItemDto updated = routingService.updateRoutingItemLineCode(
                id, request.getLineCode(), request.getUpdatedBy());
        return ResponseEntity.ok(ApiResponse.success("Updated", updated));
    }

    public static class BomExpandRequest {
        private String itemNumber;
        private BigDecimal quantity;

        public String getItemNumber() { return itemNumber; }
        public void setItemNumber(String itemNumber) { this.itemNumber = itemNumber; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    }

    public static class UpdateLineRequest {
        private String lineCode;
        private String updatedBy;

        public String getLineCode() { return lineCode; }
        public void setLineCode(String lineCode) { this.lineCode = lineCode; }
        public String getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    }
}

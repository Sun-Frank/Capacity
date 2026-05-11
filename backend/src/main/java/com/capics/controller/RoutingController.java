package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.BomExpandDto;
import com.capics.dto.RoutingDto;
import com.capics.dto.RoutingItemDto;
import com.capics.dto.WmsBomImportRequest;
import com.capics.service.RoutingService;
import com.capics.service.WmsBomImportService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routings")
public class RoutingController {

    private final RoutingService routingService;
    private final WmsBomImportService wmsBomImportService;

    public RoutingController(RoutingService routingService, WmsBomImportService wmsBomImportService) {
        this.routingService = routingService;
        this.wmsBomImportService = wmsBomImportService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAll() {
        return ResponseEntity.ok(ApiResponse.success(routingService.findAll()));
    }

    @GetMapping("/full")
    public ResponseEntity<ApiResponse> getAllFull() {
        return ResponseEntity.ok(ApiResponse.success(routingService.findAllItems()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(routingService.findById(id)));
    }

    @GetMapping("/product/{productNumber}")
    public ResponseEntity<ApiResponse> getByProductNumber(@PathVariable String productNumber) {
        return ResponseEntity.ok(ApiResponse.success(routingService.findByProductNumber(productNumber)));
    }

    @GetMapping("/by-product/{productNumber}")
    public ResponseEntity<ApiResponse> getItemsByProduct(@PathVariable String productNumber) {
        return ResponseEntity.ok(ApiResponse.success(routingService.getByProductNumber(productNumber)));
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
        return ResponseEntity.ok(ApiResponse.success("Updated", routingService.save(dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        routingService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }

    @PostMapping("/import/check")
    public ResponseEntity<ApiResponse> checkImportDuplicates(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, String>> duplicates = routingService.checkDuplicates(file);
        if (duplicates.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No duplicates found", null));
        }
        return ResponseEntity.ok(new ApiResponse(false, "Found " + duplicates.size() + " duplicate(s)", duplicates));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse> importRoutings(
            @RequestParam("file") MultipartFile file,
            @RequestParam("createdBy") String createdBy,
            @RequestParam(value = "overwrite", defaultValue = "false") boolean overwrite) throws IOException {
        int count = routingService.importFromExcel(file, createdBy, overwrite);
        return ResponseEntity.ok(ApiResponse.success("Imported " + count + " records"));
    }

    @PostMapping("/wms/import")
    public ResponseEntity<ApiResponse> importFromWms(@RequestBody WmsBomImportRequest request, Principal principal)
            throws IOException, InterruptedException {
        String operator = principal == null ? "system" : principal.getName();
        return ResponseEntity.ok(ApiResponse.success("Imported WMS BOM", wmsBomImportService.fetchAndImport(request, operator)));
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadRoutingTemplate() throws IOException {
        ByteArrayResource resource = new ByteArrayResource(buildBomTemplateBytes());
        String encoded = URLEncoder.encode("BOM结构导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .body(resource);
    }

    @GetMapping("/items/grouped")
    public ResponseEntity<ApiResponse> getItemsGroupedByLine() {
        return ResponseEntity.ok(ApiResponse.success(routingService.findAllItemsGroupedByLine()));
    }

    @PutMapping("/items/{id}/line")
    public ResponseEntity<ApiResponse> updateItemLine(@PathVariable Long id, @RequestBody UpdateLineRequest request) {
        RoutingItemDto updated = routingService.updateRoutingItemLineCode(id, request.getLineCode(), request.getUpdatedBy());
        return ResponseEntity.ok(ApiResponse.success("Updated", updated));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse> saveItem(@RequestBody RoutingItemDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Saved", routingService.saveItem(dto, dto.getUpdatedBy())));
    }

    private byte[] buildBomTemplateBytes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Data");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("成品物料号*");
            header.createCell(1).setCellValue("成品描述");
            header.createCell(2).setCellValue("组件物料号*");
            header.createCell(3).setCellValue("BOM层级*");
            header.createCell(4).setCellValue("BOM用量");

            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("FG-1001");
            sample.createCell(1).setCellValue("Product description");
            sample.createCell(2).setCellValue("COMP-1001");
            sample.createCell(3).setCellValue(1);
            sample.createCell(4).setCellValue(1);

            for (int i = 0; i <= 4; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
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

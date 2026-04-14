package com.capics.controller;

import com.capics.dto.ApiResponse;
import com.capics.dto.ProductDto;
import com.capics.dto.ProductFamilyDto;
import com.capics.dto.FamilyLineDto;
import com.capics.service.ProductFamilyService;
import com.capics.service.ProductService;
import com.capics.service.FamilyLineService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductFamilyService familyService;
    private final ProductService productService;
    private final FamilyLineService familyLineService;

    public ProductController(ProductFamilyService familyService, ProductService productService, FamilyLineService familyLineService) {
        this.familyService = familyService;
        this.productService = productService;
        this.familyLineService = familyLineService;
    }

    @GetMapping("/families")
    public ResponseEntity<ApiResponse> getAllFamilies() {
        List<ProductFamilyDto> families = familyService.findAll();
        return ResponseEntity.ok(ApiResponse.success(families));
    }

    @GetMapping("/families/search")
    public ResponseEntity<ApiResponse> searchFamilies(@RequestParam(required = false) String keyword) {
        List<ProductFamilyDto> families = familyService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(families));
    }

    @GetMapping("/families/{familyCode}/{lineCode}")
    public ResponseEntity<ApiResponse> getFamilyById(@PathVariable String familyCode, @PathVariable String lineCode) {
        ProductFamilyDto family = familyService.findById(familyCode, lineCode);
        return ResponseEntity.ok(ApiResponse.success(family));
    }

    @PostMapping("/families")
    public ResponseEntity<ApiResponse> createFamily(@RequestBody ProductFamilyDto dto) {
        ProductFamilyDto saved = familyService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Created", saved));
    }

    @PutMapping("/families/{familyCode}/{lineCode}")
    public ResponseEntity<ApiResponse> updateFamily(@PathVariable String familyCode, @PathVariable String lineCode,
                                                     @RequestBody ProductFamilyDto dto) {
        // 传递原始key（path variable）和新key（body中的值）
        ProductFamilyDto updated = familyService.save(dto, familyCode, lineCode);
        return ResponseEntity.ok(ApiResponse.success("Updated", updated));
    }

    @DeleteMapping("/families/{familyCode}/{lineCode}")
    public ResponseEntity<ApiResponse> deleteFamily(@PathVariable String familyCode, @PathVariable String lineCode) {
        familyService.delete(familyCode, lineCode);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }

    // 检查导入文件中的重复记录
    @PostMapping("/families/import/check")
    public ResponseEntity<ApiResponse> checkFamilyImportDuplicates(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, String>> duplicates = familyService.checkDuplicates(file);
        if (duplicates.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No duplicates found", null));
        } else {
            return ResponseEntity.ok(new ApiResponse(false, "Found " + duplicates.size() + " duplicate(s)", duplicates));
        }
    }

    @PostMapping("/families/import")
    public ResponseEntity<ApiResponse> importFamilies(
            @RequestParam("file") MultipartFile file,
            @RequestParam("createdBy") String createdBy) {
        try {
            int count = familyService.importFromExcel(file, createdBy);
            return ResponseEntity.ok(ApiResponse.success("Imported " + count + " records"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("导入编码族失败: " + e.getMessage()));
        }
    }

    @GetMapping("/families/template")
    public ResponseEntity<Resource> downloadFamilyTemplate() throws IOException {
        return downloadTemplateFile("编码族导入模板.xlsx");
    }

    @GetMapping("/family-lines/template")
    public ResponseEntity<Resource> downloadFamilyLineTemplate() throws IOException {
        return downloadTemplateFile("编码族定线导入模板.xlsx");
    }

    // ==================== Family Lines ====================

    @GetMapping("/family-lines")
    public ResponseEntity<ApiResponse> getAllFamilyLines() {
        List<FamilyLineDto> familyLines = familyLineService.findAll();
        return ResponseEntity.ok(ApiResponse.success(familyLines));
    }

    @GetMapping("/family-lines/search")
    public ResponseEntity<ApiResponse> searchFamilyLines(@RequestParam(required = false) String keyword) {
        List<FamilyLineDto> familyLines = familyLineService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(familyLines));
    }

    @GetMapping("/family-lines/{familyCode}/{lineCode}")
    public ResponseEntity<ApiResponse> getFamilyLineById(@PathVariable String familyCode, @PathVariable String lineCode) {
        FamilyLineDto familyLine = familyLineService.findById(familyCode, lineCode);
        return ResponseEntity.ok(ApiResponse.success(familyLine));
    }

    @PutMapping("/family-lines/{familyCode}/{lineCode}")
    public ResponseEntity<ApiResponse> updateFamilyLine(@PathVariable String familyCode, @PathVariable String lineCode,
                                                        @RequestBody FamilyLineDto dto) {
        // 传递原始key（path variable）和新key（body中的值）
        FamilyLineDto updated = familyLineService.save(dto, dto.getUpdatedBy(), familyCode, lineCode);
        return ResponseEntity.ok(ApiResponse.success("Updated", updated));
    }

    // 检查导入文件中的重复记录
    @PostMapping("/family-lines/import/check")
    public ResponseEntity<ApiResponse> checkFamilyLineImportDuplicates(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, String>> duplicates = familyLineService.checkDuplicates(file);
        if (duplicates.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No duplicates found", null));
        } else {
            return ResponseEntity.ok(new ApiResponse(false, "Found " + duplicates.size() + " duplicate(s)", duplicates));
        }
    }

    @PostMapping("/family-lines/import")
    public ResponseEntity<ApiResponse> importFamilyLines(
            @RequestParam("file") MultipartFile file,
            @RequestParam("createdBy") String createdBy) {
        try {
            int count = familyLineService.importFromExcel(file, createdBy);
            return ResponseEntity.ok(ApiResponse.success("Imported " + count + " records"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("导入编码族定线失败: " + e.getMessage()));
        }
    }

    // ==================== Products ====================

    @GetMapping
    public ResponseEntity<ApiResponse> getAllProducts() {
        List<ProductDto> products = productService.findAll();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchProducts(@RequestParam(required = false) String keyword) {
        List<ProductDto> products = productService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{itemNumber}/{lineCode}")
    public ResponseEntity<ApiResponse> getProductById(@PathVariable String itemNumber, @PathVariable String lineCode) {
        ProductDto product = productService.findById(itemNumber, lineCode);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createProduct(@RequestBody ProductDto dto) {
        ProductDto saved = productService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Created", saved));
    }

    @PutMapping("/{itemNumber}/{lineCode}")
    public ResponseEntity<ApiResponse> updateProduct(@PathVariable String itemNumber, @PathVariable String lineCode,
                                                      @RequestBody ProductDto dto) {
        dto.setItemNumber(itemNumber);
        dto.setLineCode(lineCode);
        ProductDto updated = productService.save(dto);
        return ResponseEntity.ok(ApiResponse.success("Updated", updated));
    }

    @DeleteMapping("/{itemNumber}/{lineCode}")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable String itemNumber, @PathVariable String lineCode) {
        productService.delete(itemNumber, lineCode);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }

    // 检查导入产品文件中的重复记录
    @PostMapping("/import/check")
    public ResponseEntity<ApiResponse> checkProductImportDuplicates(@RequestParam("file") MultipartFile file) throws IOException {
        List<Map<String, String>> duplicates = productService.checkDuplicates(file);
        if (duplicates.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No duplicates found", null));
        } else {
            return ResponseEntity.ok(new ApiResponse(false, "Found " + duplicates.size() + " duplicate(s)", duplicates));
        }
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse> importProducts(
            @RequestParam("file") MultipartFile file,
            @RequestParam("createdBy") String createdBy) {
        try {
            int count = productService.importFromExcel(file, createdBy);
            return ResponseEntity.ok(ApiResponse.success("Imported " + count + " records"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("导入产品失败: " + e.getMessage()));
        }
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadProductTemplate() throws IOException {
        return downloadTemplateFile("产品导入模板.xlsx");
    }

    private ResponseEntity<Resource> downloadTemplateFile(String fileName) throws IOException {
        File localFile = new File("import_templates", fileName);
        Resource resource;
        if (localFile.exists()) {
            resource = new org.springframework.core.io.FileSystemResource(localFile);
        } else {
            resource = new ClassPathResource("import_templates/" + fileName);
        }

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}

